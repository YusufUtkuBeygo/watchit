package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//Bildirimi kullanıcı oluşturmaz (POST yok), sistem oluşturur. Kullanıcı yalnız kendine gelenlerle işlem yapar
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    //Bana gelen bildirimler, en yeni en üstte
    @GetMapping
    public List<NotificationResponseDto> getMyNotifications(@AuthenticationPrincipal Long userId)
    {
        return notificationService.getMyNotifications(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> getNotificationById(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        NotificationResponseDto notification = notificationService.getNotificationById(id, userId);

        if(notification == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(notification);
    }

    //Okundu işaretle (gövde yok). Eski genel PUT kaldırıldı: gönderen/alıcı değiştirilebiliyordu
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        NotificationResponseDto notification = notificationService.markAsRead(id, userId);

        if(notification == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(notification);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificationById(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        if(!notificationService.deleteNotificationById(id, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
