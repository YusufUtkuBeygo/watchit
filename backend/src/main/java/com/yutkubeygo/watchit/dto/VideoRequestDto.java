package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoRequestDto {
    @NotBlank
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;

    @NotNull
    private Long channelId;
    @NotNull
    private Long categoryId;
}
