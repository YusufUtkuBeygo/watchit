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

    @Modifying
    @Transactional
    @Query("DELETE FROM VideoChapter c WHERE c.video.id = :videoId")
    int deleteByVideoId(@Param("videoId") Long videoId);
}
