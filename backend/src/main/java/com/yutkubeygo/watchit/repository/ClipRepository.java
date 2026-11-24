package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Clip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClipRepository extends JpaRepository<Clip, Long> {

    //Kliplerim: en yeni en üstte
    List<Clip> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
