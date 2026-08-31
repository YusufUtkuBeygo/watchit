package com.yutkubeygo.watchit.dto;

import com.yutkubeygo.watchit.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDto {
    private Long id;

    private Long receiverId;
    private Long senderId;

    private String title;
    private String message;

    private NotificationType type;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
