package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscodeResult;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

//Arka plan işçisi: videoyu medya servisine dönüştürtür, yeni dosyayı MinIO'ya yazar, videoyu ona geçirir,
//eski dosyayı siler. Sonunda (başarılı ya da değil) transkripti başlatır: Whisper hep güncel dosyayı işler,
//silinmek üzere olan eski dosyayı değil.
//Ayrı sınıf, çünkü @Async yalnız başka bir sınıftan çağrılınca çalışır.
@Component
public class VideoProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingWorker.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final VideoRepository videoRepository;
    private final VideoProcessingClient client;
    private final FileService fileService;
    private final TranscriptService transcriptService;
    private final HlsService hlsService;
    private final ThumbnailService thumbnailService;

    public VideoProcessingWorker(VideoRepository videoRepository, VideoProcessingClient client,
                                 FileService fileService, TranscriptService transcriptService, HlsService hlsService,
                                 ThumbnailService thumbnailService)
    {
        this.thumbnailService = thumbnailService;
        this.videoRepository = videoRepository;
        this.client = client;
        this.fileService = fileService;
        this.transcriptService = transcriptService;
        this.hlsService = hlsService;
    }

    @Async("videoProcessingExecutor")
    public void processAsync(Long videoId)
    {
        process(videoId);
    }

    //PENDING -> PROCESSING -> READY (yeni dosya yayında) ya da FAILED (orijinal dosya oynamaya devam eder)
    public void process(Long videoId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        //Video silindiyse ya da iş artık sırada değilse yapılacak bir şey yok
        if(video == null || video.getProcessingStatus() != VideoProcessingStatus.PENDING)
            return;

        String sourceUrl = video.getVideoUrl();
        if(videoRepository.changeProcessingStatus(videoId, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING) == 0)
            return;
        log.info("Dönüştürme başladı: video {}", videoId);

        TranscodeResult result = null;
        try
        {
            result = client.transcode(sourceUrl);
            String newUrl = fileService.uploadProcessedVideo(result.file());
            Integer duration = result.durationSeconds() > 0 ? (int) Math.round(result.durationSeconds()) : null;

            if(videoRepository.swapProcessedFile(videoId, sourceUrl, newUrl, duration, VideoProcessingStatus.READY) == 0)
            {
                //Sahibi bu arada dosyayı değiştirdi (yeni iş zaten sırada) ya da video silindi: bu sonuç artık geçersiz
                fileService.deleteOwnFile(newUrl);
                log.info("Dönüştürme sonucu atıldı: video {} bu arada değişti", videoId);
                return;
            }
            log.info("Dönüştürme bitti ({}): video {}, {}x{}, {} sn", result.mode(), videoId, result.width(), result.height(), duration);

            //Transkript hemen sıraya girer (sayfa "transkript yok" görmesin), sonra eski dosya silinir
            startTranscript(videoId, newUrl);
            //Çoklu kalite (HLS) yeni dosyadan; aynı sırada, bu işten sonra çalışır
            //Kapaksız yüklendiyse videodan kapak çıkarılır
            thumbnailService.ensureThumbnail(videoId);
            startHls(videoId);
            //Eski dosya artık kullanılmıyor (başka bir video da onu göstermiyorsa) silinir
            if(!videoRepository.existsByVideoUrl(sourceUrl))
                deleteOldFile(sourceUrl);
            return;
        }
        catch(Exception e)
        {
            String message = fit("Video dönüştürülemedi: " + e.getMessage());
            log.warn("Dönüştürme başarısız: video {}: {}", videoId, message);
            if(videoRepository.failProcessing(videoId, sourceUrl, VideoProcessingStatus.FAILED, message) == 0)
                return;   //video bu arada değişti ya da silindi
        }
        finally
        {
            if(result != null)
                deleteTempFile(result);
        }

        //Dönüştürülemedi: transkript orijinal dosyadan çıkarılır (soru-cevap yine çalışsın).
        //HLS de denenir: HLS her kaliteyi zaten baştan kodladığı için tarayıcının açamadığı biçimi de çevirebilir
        startTranscript(videoId, sourceUrl);
        thumbnailService.ensureThumbnail(videoId);
        startHls(videoId);
    }

    private void startHls(Long videoId)
    {
        try
        {
            hlsService.start(videoId);
        }
        catch(Exception e)
        {
            log.warn("HLS başlatılamadı: video {}: {}", videoId, e.getMessage());
        }
    }

    //Yalnız video hâlâ bu dosyayı gösteriyorsa (bu arada değiştiyse yeni iş kendi transkriptini başlatır)
    private void startTranscript(Long videoId, String url)
    {
        try
        {
            videoRepository.findById(videoId)
                    .filter(v -> Objects.equals(v.getVideoUrl(), url))
                    .ifPresent(transcriptService::start);
        }
        catch(Exception e)
        {
            log.warn("Transkript başlatılamadı: video {}: {}", videoId, e.getMessage());
        }
    }

    private void deleteOldFile(String url)
    {
        try
        {
            fileService.deleteOwnFile(url);
        }
        catch(Exception e)
        {
            //Video yine de çalışıyor; dosya depoda sahipsiz kalır
            log.warn("Eski video dosyası silinemedi: {}: {}", url, e.getMessage());
        }
    }

    private static void deleteTempFile(TranscodeResult result)
    {
        try
        {
            Files.deleteIfExists(result.file());
        }
        catch(IOException e)
        {
            log.warn("Geçici dosya silinemedi: {}", result.file());
        }
    }

    private static String fit(String text)
    {
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH - 1) + "…";
    }
}
