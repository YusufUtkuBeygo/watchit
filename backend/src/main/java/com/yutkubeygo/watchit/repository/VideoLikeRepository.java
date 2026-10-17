package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike,Long> {

        Long countByVideoId(Long videoId);
        Optional<VideoLike> findByUserIdAndVideoId(Long userId, Long videoId);
        //Kullanıcının kendi beğenileri (beğeni listesi gizli: yalnız sahibi görür)
        List<VideoLike> findByUserId(Long userId);

}
