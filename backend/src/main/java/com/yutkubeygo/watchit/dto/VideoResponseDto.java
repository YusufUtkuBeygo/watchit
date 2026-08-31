package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VideoResponseDto {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;
    private LocalDateTime createdAt;
}
