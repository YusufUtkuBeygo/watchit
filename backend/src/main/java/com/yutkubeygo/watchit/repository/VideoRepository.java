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

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    int incrementViewCount(@Param("id") Long videoId);

}
