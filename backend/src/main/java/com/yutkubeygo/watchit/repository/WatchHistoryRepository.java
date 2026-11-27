package com.yutkubeygo.watchit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.yutkubeygo.watchit.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory,Long> {


    Optional<WatchHistory> findByUserIdAndVideoId(Long userId, Long videoId);
    List<WatchHistory> findByVideoId(Long videoId);
    // Kullanıcının geçmişini son izleme tarihine göre azalan (yeniden eskiye) sıralar
    List<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId);

    //Geçmişim, sayfa sayfa (id ikinci sıralama: aynı anda yazılanlar sayfalar arasında kaymasın)
    Page<WatchHistory> findByUserIdOrderByLastWatchedAtDescIdDesc(Long userId, Pageable pageable);

    //Kurs ilerlemesi: bu videolardan kaçını izlemiş (geçmişte kaydı olan)
    long countByUserIdAndVideoIdIn(Long userId, Collection<Long> videoIds);

}
