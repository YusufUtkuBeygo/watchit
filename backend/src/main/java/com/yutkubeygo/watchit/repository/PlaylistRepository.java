package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Playlist;
import com.yutkubeygo.watchit.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PlaylistRepository extends JpaRepository<Playlist,Long> {
    List<Playlist> findByIsPublicTrueOrOwnerId(Long ownerId);

}
