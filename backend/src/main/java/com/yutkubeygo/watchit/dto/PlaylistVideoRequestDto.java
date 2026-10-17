package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistVideoRequestDto {

    @NotNull
    private Long playlistId;
    @NotNull
    private Long videoId;
    @Min(0)
    private Integer orderIndex;
}