package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoRequestDto {
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;

    private Long channelId;
    private Long categoryId;
}
