package com.yutkubeygo.watchit.dto;

import com.yutkubeygo.watchit.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequestDto {

    private Long receiverId;
    private Long senderId;

    private String title;
    private String message;

    private NotificationType type;

}
