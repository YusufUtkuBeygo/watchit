package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchHistoryRequestDto {

    private Long userId;
    private Long videoId;
    private Integer watchedSeconds;
    private Boolean isCompleted;

}
