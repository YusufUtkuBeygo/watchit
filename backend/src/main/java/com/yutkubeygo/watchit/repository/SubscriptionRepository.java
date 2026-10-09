package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {

    List <Subscription> findBySubscriberId(Long userId);

}
