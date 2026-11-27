package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Report;
import com.yutkubeygo.watchit.enums.ReportStatus;
import com.yutkubeygo.watchit.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    //Yönetim paneli: duruma göre, en eski bekleyen en üstte (sırayla incelensin)
    Page<Report> findByStatusOrderByCreatedAtAscIdAsc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);

    //Aynı içeriğe kaç açık şikayet var (panelde "3 şikayet" gibi gösterilir)
    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType type, Long targetId, ReportStatus status);

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType type, Long targetId);

    //İçerik kaldırılınca ona ait bütün açık şikayetler tek seferde kapanır
    @Modifying
    @Query("""
            UPDATE Report r SET r.status = :status, r.resolvedAt = :at, r.resolvedBy.id = :adminId
            WHERE r.targetType = :type AND r.targetId = :targetId AND r.status = com.yutkubeygo.watchit.enums.ReportStatus.OPEN
            """)
    int closeOpenReports(@Param("type") ReportTargetType type, @Param("targetId") Long targetId,
                         @Param("status") ReportStatus status, @Param("at") LocalDateTime at, @Param("adminId") Long adminId);
}
