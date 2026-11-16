package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.entity.Notification;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.NotificationMapper;
import com.yutkubeygo.watchit.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

//Karar (18 Ekim): bildirimi kullanıcı değil sistem oluşturur (YouTube gibi: "biri videona yorum yaptı").
//Bu yüzden POST ucu yok; bildirimleri notify/notifyAll ile diğer servisler (abonelik, yorum, beğeni, video) oluşturur.
//Kullanıcı yalnız KENDİNE gelen bildirimleri görür, okundu işaretler ve siler.
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    private static final int MAX_LENGTH = 255;

    public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    //Sistem içinden çağrılır (controller'dan değil): abonelik, yorum, beğeni ve video yükleme servisleri kullanır.
    //Çağıran metot @Transactional olduğu için bildirim, asıl kayıtla aynı transaction'da yazılır:
    //biri başarısız olursa ikisi de geri alınır.
    //Kişi kendi işlemi için bildirim almaz (kendi videosuna yorum, kendi yorumuna beğeni...).
    public void notify(User sender, User receiver, NotificationType type, String title, String message)
    {
        if(sender.getId().equals(receiver.getId()))
            return;

        notificationRepository.save(new Notification(fit(title), sender, receiver, fit(message), type));
    }

    //Aynı bildirimi birden çok alıcıya yazar (yeni videoda kanalın bütün abonelerine)
    public void notifyAll(User sender, List<User> receivers, NotificationType type, String title, String message)
    {
        List<Notification> notifications = receivers.stream()
                .filter(receiver -> !receiver.getId().equals(sender.getId()))
                .map(receiver -> new Notification(fit(title), sender, receiver, fit(message), type))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    //Sütunlar varchar(255): uzun video başlığı mesajı taşırırsa kısaltılır (yoksa veritabanı hatası olurdu)
    private String fit(String text)
    {
        if(text.length() <= MAX_LENGTH)
            return text;

        return text.substring(0, MAX_LENGTH - 1) + "…";
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
