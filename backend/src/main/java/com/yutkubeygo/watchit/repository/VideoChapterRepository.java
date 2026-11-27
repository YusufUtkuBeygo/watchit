package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.VideoChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VideoChapterRepository extends JpaRepository<VideoChapter, Long> {

    List<VideoChapter> findByVideoIdOrderByStartSecondsAsc(Long videoId);

    //Karesi eksik bölümü olan videolar (açılışta tamamlanır)
    @Query("SELECT DISTINCT c.video.id FROM VideoChapter c WHERE c.thumbnailUrl IS NULL")
    List<Long> findVideoIdsWithMissingFrames();

    //Kareyi yalnız bölüm hâlâ duruyorsa ve karesi yoksa yazar. 0 = bölüm bu arada silindi/yenilendi (kare boşa çıktı, silinmeli)
    @Modifying
    @Transactional
    @Query("UPDATE VideoChapter c SET c.thumbnailUrl = :url WHERE c.id = :id AND c.thumbnailUrl IS NULL")
    int setThumbnailIfMissing(@Param("id") Long id, @Param("url") String url);

    @Modifying
    @Transactional
    @Query("DELETE FROM VideoChapter c WHERE c.video.id = :videoId")
    int deleteByVideoId(@Param("videoId") Long videoId);
}
