package com.yutkubeygo.watchit.repository;


import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {

    //Kanalın videoları, en yeni en üstte (video.channel.id = ?)
    List<Video> findByChannelIdOrderByCreatedAtDesc(Long channelId);

    //Kategorinin videoları, en yeni en üstte (video.category.id = ?)
    List<Video> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    //Kanalın video sayısı (kanal sayfası için)
    long countByChannelId(Long channelId);

    //Bütün videolar, en yeni en üstte
    List<Video> findAllByOrderByCreatedAtDesc();

    //Başlıkta arama, Türkçe harf ve büyük/küçük harf fark etmez: "istanbul", "ISTANBUL", "İstanbul" hepsi eşleşir; en yeni en üstte.
    //Hazır IgnoreCase upper() kullanıyor ve İ/i'yi eşleştiremiyor, bu yüzden elle yazılmış SQL (nativeQuery).
    //unaccent: İ/I/ı -> i, ş -> s, ç -> c... (V10). text içindeki %, _ ve \ önceden kaçırılmış olmalı (VideoService.searchVideos).
    @Query(value = "SELECT * FROM videos WHERE lower(unaccent(title)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\' ORDER BY created_at DESC",
            nativeQuery = true)
    List<Video> searchByTitle(@Param("text") String text);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    int incrementViewCount(@Param("id") Long videoId);

    //--- Video dönüştürme ---
    //Durum değişiklikleri tek UPDATE ile: videonun tamamını save etmek, o sırada sahibinin yaptığı
    //değişikliği (başlık, açıklama) eski hâliyle ezerdi (kayıp güncelleme)

    //Açılışta yarım kalan işler
    List<Video> findByProcessingStatusIn(List<VideoProcessingStatus> statuses);

    //Bu dosyayı kullanan başka video var mı (eski dosyayı silmeden önce)
    boolean existsByVideoUrl(String videoUrl);

    //İşi üstlen: yalnız hâlâ sıradaysa (PENDING -> PROCESSING). 0 = başkası aldı ya da iptal oldu.
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.processingStatus = :to WHERE v.id = :id AND v.processingStatus = :from")
    int changeProcessingStatus(@Param("id") Long id, @Param("from") VideoProcessingStatus from, @Param("to") VideoProcessingStatus to);

    //Dönüşmüş dosyaya geç: yalnız video hâlâ aynı dosyayı gösteriyorsa (sahibi bu arada dosyayı değiştirdiyse 0 döner)
    @Modifying
    @Transactional
    @Query("""
            UPDATE Video v SET v.videoUrl = :newUrl,
                               v.durationInSeconds = COALESCE(:duration, v.durationInSeconds),
                               v.processingStatus = :status, v.processingError = NULL
            WHERE v.id = :id AND v.videoUrl = :oldUrl
            """)
    int swapProcessedFile(@Param("id") Long id, @Param("oldUrl") String oldUrl, @Param("newUrl") String newUrl,
                          @Param("duration") Integer duration, @Param("status") VideoProcessingStatus status);

    //Dönüştürülemedi: yalnız video hâlâ aynı dosyayı gösteriyorsa
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.processingStatus = :status, v.processingError = :error WHERE v.id = :id AND v.videoUrl = :url")
    int failProcessing(@Param("id") Long id, @Param("url") String url, @Param("status") VideoProcessingStatus status,
                       @Param("error") String error);

    //--- HLS (çoklu kalite) ---

    List<Video> findByHlsStatusIn(List<VideoProcessingStatus> statuses);

    //Eski videolar: dosyası olan, dönüştürmesi sürmeyen ve HLS'si hiç sıraya girmemiş ya da eskimiş (başka dosyadan üretilmiş)
    //olanlar. FAILED olanlar kendiliğinden yeniden denenmez.
    @Query("""
            SELECT v FROM Video v
            WHERE v.videoUrl IS NOT NULL
              AND (v.processingStatus IS NULL OR v.processingStatus IN :doneStatuses)
              AND (v.hlsStatus IS NULL OR (v.hlsStatus = :ready AND (v.hlsSourceUrl IS NULL OR v.hlsSourceUrl <> v.videoUrl)))
            ORDER BY v.id
            """)
    List<Video> findHlsBackfillCandidates(@Param("doneStatuses") List<VideoProcessingStatus> doneStatuses,
                                          @Param("ready") VideoProcessingStatus ready);

    //Sıraya al: kaynak dosya hangisiyse o yazılır; işçi yalnız bu dosya hâlâ geçerliyse çalışır
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.hlsStatus = :status, v.hlsError = null WHERE v.id = :id")
    int queueHls(@Param("id") Long id, @Param("status") VideoProcessingStatus status);

    //İşi üstlen: yalnız hâlâ sıradaysa (PENDING -> PROCESSING)
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.hlsStatus = :to WHERE v.id = :id AND v.hlsStatus = :from")
    int changeHlsStatus(@Param("id") Long id, @Param("from") VideoProcessingStatus from, @Param("to") VideoProcessingStatus to);

    //HLS hazır: yalnız video hâlâ aynı dosyayı gösteriyorsa (bu arada dosya değiştiyse 0, sonuç atılır)
    @Modifying
    @Transactional
    @Query("""
            UPDATE Video v SET v.hlsUrl = :hlsUrl, v.hlsSourceUrl = :sourceUrl, v.hlsStatus = :status, v.hlsError = null
            WHERE v.id = :id AND v.videoUrl = :sourceUrl
            """)
    int finishHls(@Param("id") Long id, @Param("sourceUrl") String sourceUrl, @Param("hlsUrl") String hlsUrl,
                  @Param("status") VideoProcessingStatus status);

    //HLS üretilemedi: video MP4'ten oynamaya devam eder
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.hlsStatus = :status, v.hlsError = :error WHERE v.id = :id AND v.videoUrl = :sourceUrl")
    int failHls(@Param("id") Long id, @Param("sourceUrl") String sourceUrl, @Param("status") VideoProcessingStatus status,
                @Param("error") String error);
}
