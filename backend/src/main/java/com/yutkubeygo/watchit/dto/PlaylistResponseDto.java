package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
 @Getter
 @Setter
public class PlaylistResponseDto {

    private Long id;
    private String title;
    private String description;
    private Boolean isPublic;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
