package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike,Long> {

    //Kullanıcının kendi yorum beğenileri (beğeni listesi gizli: yalnız sahibi görür)
    List<CommentLike> findByUserId(Long userId);

}
