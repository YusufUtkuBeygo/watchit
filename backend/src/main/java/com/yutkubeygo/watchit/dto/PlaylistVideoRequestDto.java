package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistVideoRequestDto {

    @NotNull
    private Long playlistId;
    @NotNull
    private Long videoId;
    private Integer orderIndex;
}