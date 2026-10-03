package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoLikeRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long videoId;
}
