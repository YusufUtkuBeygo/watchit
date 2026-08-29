package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.PlaylistVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo,Long> {

}
