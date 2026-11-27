package com.yutkubeygo.watchit.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;
import com.yutkubeygo.watchit.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike,Long> {

    //Kullanıcının kendi yorum beğenileri (beğeni listesi gizli: yalnız sahibi görür)
    List<CommentLike> findByUserId(Long userId);

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    //Yorum listesindeki beğeni sayıları tek sorguda: [yorum id, sayı]
    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :ids GROUP BY cl.comment.id")
    List<Object[]> countByCommentIds(@Param("ids") Collection<Long> ids);

    //Bu listede benim beğendiğim yorumlar
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

}
