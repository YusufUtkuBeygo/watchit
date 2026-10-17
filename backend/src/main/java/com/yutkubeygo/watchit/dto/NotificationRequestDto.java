package com.yutkubeygo.watchit.dto;

import com.yutkubeygo.watchit.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequestDto {

    @NotNull
    private Long receiverId;
    @NotNull
    private Long senderId;

    @Size(max = 255)
    private String title;
    @Size(max = 255)
    private String message;

    @NotNull
    private NotificationType type;

}
