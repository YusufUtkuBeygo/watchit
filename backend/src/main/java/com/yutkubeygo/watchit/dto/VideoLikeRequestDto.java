package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoLikeRequestDto {
    private Long userId;
    private Long videoId;
}
