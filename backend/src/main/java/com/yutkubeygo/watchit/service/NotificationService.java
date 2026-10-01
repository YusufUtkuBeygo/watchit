package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.entity.Notification;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.mapper.NotificationMapper;
import com.yutkubeygo.watchit.repository.NotificationRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public NotificationResponseDto updateNotification(Long id ,NotificationRequestDto notificationRequestDto)
    {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if(notification==null)
            return null;

        User sender = userRepository.findById(notificationRequestDto.getSenderId()).orElse(null);
        if(sender==null)
            return null;

        User receiver = userRepository.findById(notificationRequestDto.getReceiverId()).orElse(null);
        if(receiver==null)
            return null;

        notification.setSender(sender);
        notification.setReceiver(receiver);
        notification.setTitle(notificationRequestDto.getTitle());
        notification.setMessage(notificationRequestDto.getMessage());
        notification.setType(notificationRequestDto.getType());

        return notificationMapper.toDto(notificationRepository.save(notification));

    }

    public NotificationResponseDto getNotificationById(Long id)
    {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if(notification==null)
            return null;

        return notificationMapper.toDto(notification);
    }

    public List<NotificationResponseDto> getAllNotifications()
    {
        return notificationMapper.toDtoList(notificationRepository.findAll());
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteNotificationById(Long id)
    {
        if(!notificationRepository.existsById(id))
            return false;

        notificationRepository.deleteById(id);
        return true;
    }
}
