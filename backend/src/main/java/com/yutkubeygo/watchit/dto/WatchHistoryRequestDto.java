package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchHistoryRequestDto {

    @NotNull
    private Long videoId;
    @Min(0)
    private Integer watchedSeconds;
    private Boolean isCompleted;

}
