package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike,Long> {

}
