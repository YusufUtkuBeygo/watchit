package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.UploadReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface UploadReservationRepository extends JpaRepository<UploadReservation, Long> {

    //Kullanıcının verilen andan bu yana aldığı yükleme adreslerinin toplam boyutu (günlük kota)
    @Query("SELECT COALESCE(SUM(r.bytes), 0) FROM UploadReservation r WHERE r.userId = :userId AND r.createdAt > :since")
    long sumBytesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    //Günlük kota yalnız son 24 saate baktığı için eski satırlar gereksiz (temizlik işi siler)
    @Modifying
    @Transactional
    @Query("DELETE FROM UploadReservation r WHERE r.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
