package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.StorageQuotaDto;
import com.yutkubeygo.watchit.entity.UploadReservation;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.QuotaExceededException;
import com.yutkubeygo.watchit.repository.UploadReservationRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

//Yükleme kotası. Neden: dosyalar Spring'den geçmeden doğrudan MinIO'ya gidiyor; sınır olmasa tek bir kullanıcı
//saatler içinde sunucunun diskini doldurabilirdi.
//İki sınır:
//  toplam:  kanalındaki videoların boyutları + yeni dosya <= upload.quota.total-bytes (video silinince yer açılır)
//  günlük:  son 24 saatte aldığı yükleme adreslerinin boyutları + yeni dosya <= upload.quota.daily-bytes
//           (videoya bağlanmayan dosyalar da sayılır; onları da sahipsiz dosya temizliği bir gün sonra siler)
@Service
public class UploadQuotaService {

    private final UploadReservationRepository reservationRepository;
    private final VideoRepository videoRepository;
    private final long maxFileBytes;
    private final long totalBytes;
    private final long dailyBytes;

    public UploadQuotaService(UploadReservationRepository reservationRepository, VideoRepository videoRepository,
                              @Value("${upload.max-file-bytes}") long maxFileBytes,
                              @Value("${upload.quota.total-bytes}") long totalBytes,
                              @Value("${upload.quota.daily-bytes}") long dailyBytes)
    {
        this.reservationRepository = reservationRepository;
        this.videoRepository = videoRepository;
        this.maxFileBytes = maxFileBytes;
        this.totalBytes = totalBytes;
        this.dailyBytes = dailyBytes;
    }

    public long getMaxFileBytes()
    {
        return maxFileBytes;
    }

    //Yer varsa deftere yazar; yoksa QuotaExceededException (413). Boyut aralık dışıysa BadRequestException (400).
    //synchronized: aynı kullanıcının aynı anda gelen iki isteği ikisi de "yer var" görüp sınırı birlikte aşmasın
    //(tek sunucu için yeterli; birden çok sunucuda veritabanı kilidi gerekirdi).
    //@Transactional bilerek yok: save kendi işleminde kilit içindeyken commit olur, sıradaki istek onu görür
    public synchronized void reserve(Long userId, long fileBytes, boolean isVideo)
    {
        if(fileBytes < 1 || fileBytes > maxFileBytes)
            throw new BadRequestException();

        //Toplam kotaya yalnız videolar bakar: kapak ve kanal resmi küçük, onlar günlük sınırla yeter
        if(isVideo && videoRepository.sumSizeBytesByOwnerId(userId) + fileBytes > totalBytes)
            throw new QuotaExceededException(QuotaExceededException.Limit.TOTAL);

        if(dailyUsed(userId) + fileBytes > dailyBytes)
            throw new QuotaExceededException(QuotaExceededException.Limit.DAILY);

        reservationRepository.save(new UploadReservation(userId, fileBytes));
    }

    //Stüdyo'daki "Depolama" satırı
    public StorageQuotaDto getQuota(Long userId)
    {
        return new StorageQuotaDto(videoRepository.sumSizeBytesByOwnerId(userId), totalBytes,
                dailyUsed(userId), dailyBytes, maxFileBytes);
    }

    private long dailyUsed(Long userId)
    {
        return reservationRepository.sumBytesSince(userId, LocalDateTime.now().minusHours(24));
    }

    //Günde bir: 2 günden eski defter satırları (günlük kota yalnız son 24 saate bakıyor)
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000L, initialDelay = 60 * 60 * 1000L)
    public void cleanOldReservations()
    {
        reservationRepository.deleteOlderThan(LocalDateTime.now().minusDays(2));
    }
}
