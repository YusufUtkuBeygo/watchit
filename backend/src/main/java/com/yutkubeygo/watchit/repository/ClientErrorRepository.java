package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.ClientError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ClientErrorRepository extends JpaRepository<ClientError, Long> {

    Page<ClientError> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime since);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClientError e WHERE e.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
