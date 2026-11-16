package com.yutkubeygo.watchit.repository;


import com.yutkubeygo.watchit.entity.Video;
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

}
