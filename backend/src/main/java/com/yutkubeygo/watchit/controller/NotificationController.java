package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<NotificationResponseDto> createNotification(@Valid @RequestBody NotificationRequestDto notificationRequestDto)
    {
        NotificationResponseDto notification = notificationService.createNotification(notificationRequestDto);

        //Servis null döndüyse istekteki gönderen ya da alıcı kullanıcı bulunamamıştır
        if(notification == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> updateNotification(@PathVariable Long id, @Valid @RequestBody NotificationRequestDto notificationRequestDto)
    {
        NotificationResponseDto notification = notificationService.updateNotification(id, notificationRequestDto);

        if(notification == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(notification);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> getNotificationById(@PathVariable Long id )
    {
        NotificationResponseDto notification = notificationService.getNotificationById(id);

        if(notification == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(notification);
    }

    @GetMapping
    public List<NotificationResponseDto> getAllNotifications()
    {
        return notificationService.getAllNotifications();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificationById(@PathVariable Long id)
    {
        if(!notificationService.deleteNotificationById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
