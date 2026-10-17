package com.yutkubeygo.watchit.repository;


import com.yutkubeygo.watchit.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {

    //Kanalın videoları, en yeni en üstte (video.channel.id = ?)
    List<Video> findByChannelIdOrderByCreatedAtDesc(Long channelId);

}
