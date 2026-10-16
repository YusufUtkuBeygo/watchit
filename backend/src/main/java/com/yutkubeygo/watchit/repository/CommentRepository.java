package com.yutkubeygo.watchit.repository;

import  com.yutkubeygo.watchit.entity.Comment;
import  org.springframework.data.jpa.repository.JpaRepository;
import  org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {


    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

}
