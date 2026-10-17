package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.entity.Notification;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.NotificationMapper;
import com.yutkubeygo.watchit.repository.NotificationRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

//Karar (18 Ekim): bildirimi kullanıcı değil sistem oluşturur (YouTube gibi: "biri videona yorum yaptı").
//Bu yüzden POST ucu yok; createNotification ileride diğer servislerden (yorum, abonelik...) çağrılacak.
//Kullanıcı yalnız KENDİNE gelen bildirimleri görür, okundu işaretler ve siler.
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(UserRepository userRepository, NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    //Sistem içinden çağrılır (controller'dan değil): gönderen ve alıcı id'leri kodun kendisinden gelir
    public NotificationResponseDto createNotification(NotificationRequestDto notificationRequestDto)
    {
        User sender = userRepository.findById(notificationRequestDto.getSenderId()).orElse(null);
        if(sender==null)
            return null;

        User receiver = userRepository.findById(notificationRequestDto.getReceiverId()).orElse(null);
        if(receiver==null)
            return null;

        Notification notification = notificationMapper.toEntity(notificationRequestDto);

        notification.setSender(sender);
        notification.setReceiver(receiver);

        return notificationMapper.toDto(notificationRepository.save(notification));

    }

    //Yalnız bana gelenler: sorgu baştan alıcı id'siyle yapılır, kontrol gerekmez
    public List<NotificationResponseDto> getMyNotifications(Long userId)
    {
        return notificationMapper.toDtoList(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId));
    }

    //Bildirim yoksa null (404), başkasına gelmişse ForbiddenException (403)
    public NotificationResponseDto getNotificationById(Long id, Long userId)
    {
        Notification notification = findMine(id, userId);
        if(notification==null)
            return null;

        return notificationMapper.toDto(notification);
    }

    //Okundu işaretler; gönderen, alıcı, başlık gibi alanlar değiştirilemez
    public NotificationResponseDto markAsRead(Long id, Long userId)
    {
        Notification notification = findMine(id, userId);
        if(notification==null)
            return null;

        notification.setIsRead(true);
        return notificationMapper.toDto(notificationRepository.save(notification));
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Bildirim başkasına gelmişse ForbiddenException fırlatır (403)
    public boolean deleteNotificationById(Long id, Long userId)
    {
        Notification notification = findMine(id, userId);
        if(notification==null)
            return false;

        notificationRepository.delete(notification);
        return true;
    }

    //Bildirimi bulur; alıcısı token'daki kullanıcı değilse 403
    private Notification findMine(Long id, Long userId)
    {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if(notification==null)
            return null;

        if(!notification.getReceiver().getId().equals(userId))
            throw new ForbiddenException();

        return notification;
    }
}
