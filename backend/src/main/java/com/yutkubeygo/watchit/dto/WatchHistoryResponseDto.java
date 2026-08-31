package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WatchHistoryResponseDto {

    private Long id;
    private Long userId;
    private Long videoId;
    private Integer watchedSeconds;
    private Boolean isCompleted;
    private LocalDateTime lastWatchedAt;

}
