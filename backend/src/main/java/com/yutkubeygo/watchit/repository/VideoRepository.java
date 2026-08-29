package com.yutkubeygo.watchit.repository;


import com.yutkubeygo.watchit.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {

}
