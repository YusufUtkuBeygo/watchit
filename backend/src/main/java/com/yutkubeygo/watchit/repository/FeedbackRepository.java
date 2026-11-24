package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    //Saatlik sınır için: bu kullanıcı şu andan beri kaç tane gönderdi
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
