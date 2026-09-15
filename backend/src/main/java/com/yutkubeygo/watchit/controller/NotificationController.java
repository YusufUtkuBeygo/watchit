package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponseDto createNotification(@RequestBody NotificationRequestDto notificationRequestDto)
    {
        return notificationService.createNotification(notificationRequestDto);
    }

    @PutMapping("/{id}")
    public NotificationResponseDto updateNotification(@PathVariable Long id, @RequestBody NotificationRequestDto notificationRequestDto)
    {
        return notificationService.updateNotification(id, notificationRequestDto);
    }

    @GetMapping("/{id}")
    public NotificationResponseDto getNotificationById(@PathVariable Long id )
    {
        return notificationService.getNotificationById(id);
    }

    @GetMapping
    public List<NotificationResponseDto> getAllNotifications()
    {
        return notificationService.getAllNotifications();
    }

    @DeleteMapping("/{id}")
    public void deleteNotificationById(@PathVariable Long id)
    {
        notificationService.deleteNotificationById(id);
    }

}
