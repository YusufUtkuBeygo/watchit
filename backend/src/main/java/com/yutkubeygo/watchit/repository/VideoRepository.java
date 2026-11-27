package com.yutkubeygo.watchit.repository;


import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long>, JpaSpecificationExecutor<Video> {

    //Kanalın videoları, en yeni en üstte (video.channel.id = ?)
    List<Video> findByChannelIdOrderByCreatedAtDesc(Long channelId);

    //Kategorinin videoları, en yeni en üstte (video.category.id = ?)
    List<Video> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    //Kanalın video sayısı (kanal sayfası için)
    long countByChannelId(Long channelId);

    //Bütün videolar, en yeni en üstte
    List<Video> findAllByOrderByCreatedAtDesc();

    //Arama: başlıkta, açıklamada ya da kanal adında geçenler. Türkçe harf ve büyük/küçük harf fark etmez
    //("istanbul", "ISTANBUL", "İstanbul" hepsi eşleşir). Hazır IgnoreCase upper() kullanıyor ve İ/i'yi eşleştiremiyor: elle SQL.
    //unaccent: İ/I/ı -> i, ş -> s, ç -> c... (V10). text içindeki %, _ ve \ önceden kaçırılmış olmalı (VideoService.searchVideos).
    //Sıralama: başlıkta geçenler önce, sonra kanal adında, sonra yalnız açıklamada; her grupta en yeni üstte.
    //Sayfalı: toplam da sayılır (countQuery); id son sıralama, aynı saniyede yüklenenler sayfalar arasında kaymasın
    @Query(value = """
            SELECT v.* FROM videos v JOIN channels c ON c.id = v.channel_id
            WHERE lower(unaccent(v.title)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
               OR lower(unaccent(coalesce(v.description, ''))) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
               OR lower(unaccent(c.channel_name)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
            ORDER BY CASE WHEN lower(unaccent(v.title)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\' THEN 0
                          WHEN lower(unaccent(c.channel_name)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\' THEN 1
                          ELSE 2 END,
                     v.created_at DESC, v.id DESC
            """,
            countQuery = """
            SELECT count(*) FROM videos v JOIN channels c ON c.id = v.channel_id
            WHERE lower(unaccent(v.title)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
               OR lower(unaccent(coalesce(v.description, ''))) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
               OR lower(unaccent(c.channel_name)) LIKE '%' || lower(unaccent(:text)) || '%' ESCAPE '\\'
            """,
            nativeQuery = true)
    Page<Video> search(@Param("text") String text, Pageable pageable);

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

    //Silinecek kapak ya da HLS paketi başka bir videoda da kullanılıyor mu (StorageCleanupService)
    boolean existsByThumbnailUrl(String thumbnailUrl);
    boolean existsByHlsUrl(String hlsUrl);

    //"Senin için" sıralaması: her videoya puan verilir, büyükten küçüğe. Basit ve açıklanabilir bir formül (makine öğrenmesi yok):
    //  + ln(1 + görüntülenme)              popüler olan biraz öne (logaritma: 10 bin izlenen, 100 izlenenin 100 katı değil ~2 katı puan alır)
    //  + 3   abone olduğum kanalsa
    //  + 0,6 x (o kategoride izlediğim video sayısı, en fazla 5)   ilgi alanım
    //  - yaş (gün) / 14                     her iki haftada 1 puan düşer: yeni videolar öne
    //  - 5 bitirdiysem, - 1,5 yarım bıraktıysam (zaten gördüm)
    //  - 3 kendi videomsa
    //Girişsizde (userId null) kişisel kısımlar 0 olur: popülerlik + yenilik kalır. categoryId verilirse yalnız o kategori.
    @Query(value = """
            SELECT v.* FROM videos v
            JOIN channels ch ON ch.id = v.channel_id
            LEFT JOIN subscriptions s ON s.channel_id = v.channel_id AND s.subscriber_id = CAST(:userId AS bigint)
            LEFT JOIN watch_history_log h ON h.video_id = v.id AND h.user_id = CAST(:userId AS bigint)
            LEFT JOIN (SELECT wv.category, count(*) AS cnt FROM watch_history_log w JOIN videos wv ON wv.id = w.video_id
                       WHERE w.user_id = CAST(:userId AS bigint) GROUP BY wv.category) interest ON interest.category = v.category
            WHERE CAST(:categoryId AS bigint) IS NULL OR v.category = CAST(:categoryId AS bigint)
            ORDER BY ( ln(1 + v.view_count)
                     + CASE WHEN s.id IS NOT NULL THEN 3 ELSE 0 END
                     + LEAST(coalesce(interest.cnt, 0), 5) * 0.6
                     - EXTRACT(EPOCH FROM (now() - v.created_at)) / 86400.0 / 14
                     - CASE WHEN h.is_completed THEN 5 WHEN h.id IS NOT NULL THEN 1.5 ELSE 0 END
                     - CASE WHEN ch.owner_id = CAST(:userId AS bigint) THEN 3 ELSE 0 END ) DESC,
                     v.id DESC
            """,
            countQuery = "SELECT count(*) FROM videos v WHERE CAST(:categoryId AS bigint) IS NULL OR v.category = CAST(:categoryId AS bigint)",
            nativeQuery = true)
    Page<Video> findForYou(@Param("userId") Long userId, @Param("categoryId") Long categoryId, Pageable pageable);

    //Günlük sayaç (V25): o günün satırı yoksa 1 ile açılır, varsa 1 artar. Tek sorgu (upsert): iki izleyici aynı anda gelse de kayıp olmaz
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO video_view_daily (video_id, day, views) VALUES (:id, CURRENT_DATE, 1)
            ON CONFLICT (video_id, day) DO UPDATE SET views = video_view_daily.views + 1
            """, nativeQuery = true)
    int incrementDailyViews(@Param("id") Long videoId);

    //Kanalın son günlerdeki günlük görüntülenmeleri: [gün, toplam] (izlenme olmayan günler satır olarak gelmez)
    @Query(value = """
            SELECT d.day, SUM(d.views) FROM video_view_daily d JOIN videos v ON v.id = d.video_id
            WHERE v.channel_id = :channelId AND d.day >= :since
            GROUP BY d.day ORDER BY d.day
            """, nativeQuery = true)
    List<Object[]> dailyViewsOfChannel(@Param("channelId") Long channelId, @Param("since") java.time.LocalDate since);

    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v WHERE v.channel.id = :channelId")
    long totalViewsOfChannel(@Param("channelId") Long channelId);

    //Otomatik kapak: yalnız video hâlâ kapaksızsa yazılır (sahibi bu arada kendi kapağını eklediyse 0 döner)
    @Modifying
    @Transactional
    @Query(value = "UPDATE videos SET thumbnail_url = :url WHERE id = :id AND (thumbnail_url IS NULL OR thumbnail_url = '')", nativeQuery = true)
    int setThumbnailIfMissing(@Param("id") Long videoId, @Param("url") String url);

    //Yorumu sabitle (commentId null: sabiti kaldır)
    @Modifying
    @Transactional
    @Query(value = "UPDATE videos SET pinned_comment_id = :commentId WHERE id = :id", nativeQuery = true)
    int setPinnedComment(@Param("id") Long videoId, @Param("commentId") Long commentId);

    //Videoyu tek DELETE ile siler; yorumlar, beğeniler, transkript, klipler... veritabanında zincirle gider (ON DELETE CASCADE).
    //Neden delete(video) değil: o yol Hibernate'in hafızasındaki bağlı kayıtları (ör. aynı işlemde yüklenmiş bir yorum) da
    //düşünmek zorunda kalıyor ve "silinmiş videoya bağlı yorum" diye işlemi durduruyordu.
    //flushAutomatically: önce bekleyen yazmalar (ör. bildirim) gider; clearAutomatically: sonra hafıza temizlenir, eski kayıt kalmaz
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Video v WHERE v.id = :id")
    int deleteVideoById(@Param("id") Long id);

    //Hesap silinirken: kullanıcının videolarının yalnız dosya adresleri. Entity değil projeksiyon (yalnız bu sütunlar okunur):
    //entity yüklenseydi Hibernate onu hafızasında tutar, kullanıcı silinince "sahibi silinmiş kayıt" diye işlemi durdururdu
    List<VideoFiles> findFilesByChannelOwnerId(Long ownerId);

    //Kullanıcının kanalındaki videoların toplam boyutu (toplam yükleme kotası)
    @Query("SELECT COALESCE(SUM(v.sizeBytes), 0) FROM Video v WHERE v.channel.owner.id = :userId")
    long sumSizeBytesByOwnerId(@Param("userId") Long userId);

    //Sahipsiz dosya temizliği: dosya HLS'nin kaynağı olarak hâlâ kayıtlı mı
    boolean existsByHlsSourceUrl(String hlsSourceUrl);

    interface VideoFiles {
        Long getId();
        String getVideoUrl();
        String getThumbnailUrl();
        String getHlsSourceUrl();
        String getHlsUrl();
    }

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
