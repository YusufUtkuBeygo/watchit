package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.WatchLater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchLaterRepository extends JpaRepository<WatchLater, Long> {

    //Listem: son eklenen en üstte (aynı anda eklenenlerde sonra eklenen önce)
    List<WatchLater> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<WatchLater> findByUserIdAndVideoId(Long userId, Long videoId);

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
}
