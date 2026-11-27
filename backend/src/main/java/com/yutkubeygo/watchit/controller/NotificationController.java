package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
import java.util.Map;
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
    public PageResponseDto<NotificationResponseDto> getMyNotifications(@AuthenticationPrincipal Long userId,
                                                                       @RequestParam(required = false) Integer page,
                                                                       @RequestParam(required = false) Integer size)
    {
        return notificationService.getMyNotifications(userId, Paging.of(page, size));
    }

    //PUT /notifications/read-all: bana gelen bütün okunmamışlar okundu olur, 204
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Long userId)
    {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    //GET /notifications/unread-count -> {"count": 3}
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal Long userId)
    {
        return Map.of("count", notificationService.countUnread(userId));
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
