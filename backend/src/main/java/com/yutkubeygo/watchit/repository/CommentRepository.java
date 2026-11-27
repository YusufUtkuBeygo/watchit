package com.yutkubeygo.watchit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import  com.yutkubeygo.watchit.entity.Comment;
import  org.springframework.data.jpa.repository.JpaRepository;
import  org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {


    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    //Yorumlar sayfa sayfa: sayfalanan yalnız üst yorumlar, cevapları ayrıca hepsi gelir (cevabı yarım kalan konuşma olmasın)
    Page<Comment> findByVideoIdAndParentCommentIsNull(Long videoId, Pageable pageable);

    //Üst yorumlar, sabitlenmiş olan hariç (o ilk sayfada en üstte ayrıca gelir). excludeId yoksa -1 verilir
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT c FROM Comment c WHERE c.video.id = :videoId AND c.parentComment IS NULL AND c.id <> :excludeId
            ORDER BY c.createdAt DESC, c.id DESC""",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.video.id = :videoId AND c.parentComment IS NULL AND c.id <> :excludeId")
    Page<Comment> findNewestTopLevel(@org.springframework.data.repository.query.Param("videoId") Long videoId,
                                     @org.springframework.data.repository.query.Param("excludeId") Long excludeId, Pageable pageable);

    //En beğenilen üst yorumlar (eşitlikte yeni olan önce)
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT c FROM Comment c WHERE c.video.id = :videoId AND c.parentComment IS NULL AND c.id <> :excludeId
            ORDER BY SIZE(c.commentLikes) DESC, c.createdAt DESC, c.id DESC""",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.video.id = :videoId AND c.parentComment IS NULL AND c.id <> :excludeId")
    Page<Comment> findTopTopLevel(@org.springframework.data.repository.query.Param("videoId") Long videoId,
                                  @org.springframework.data.repository.query.Param("excludeId") Long excludeId, Pageable pageable);
    List<Comment> findByParentCommentIdInOrderByCreatedAtDesc(List<Long> parentIds);

    long countByVideoId(Long videoId);

    //Tek DELETE (cevaplar ve beğeniler veritabanında zincirle gider); sebebi VideoRepository.deleteVideoById ile aynı
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM Comment c WHERE c.id = :id")
    int deleteCommentById(@org.springframework.data.repository.query.Param("id") Long id);

}
