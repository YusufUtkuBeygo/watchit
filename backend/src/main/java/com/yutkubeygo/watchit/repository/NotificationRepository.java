package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    //Kullanıcıya gelen bildirimler, en yeni en üstte (notification.receiver.id = ?)
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

}
