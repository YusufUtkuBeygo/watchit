package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchHistoryRequestDto {

    @NotNull
    private Long userId;
    @NotNull
    private Long videoId;
    private Integer watchedSeconds;
    private Boolean isCompleted;

}
