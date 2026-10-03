package com.yutkubeygo.watchit.dto;

import com.yutkubeygo.watchit.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequestDto {

    @NotNull
    private Long receiverId;
    @NotNull
    private Long senderId;

    private String title;
    private String message;

    @NotNull
    private NotificationType type;

}
