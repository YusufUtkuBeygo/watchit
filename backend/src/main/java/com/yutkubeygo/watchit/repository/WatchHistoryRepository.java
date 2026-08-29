package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory,Long> {


    Optional<WatchHistory> findByUserIdAndVideoId(Long userId, Long videoId);
    List<WatchHistory> findByVideoId(Long videoId);
    // Kullanıcının geçmişini son izleme tarihine göre azalan (yeniden eskiye) sıralar
    List<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId);

}
