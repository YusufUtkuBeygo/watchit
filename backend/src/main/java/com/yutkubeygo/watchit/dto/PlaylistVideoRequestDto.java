package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistVideoRequestDto {

    private Long playlistId;
    private Long videoId;
    private Integer orderIndex;
}