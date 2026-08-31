package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionResponseDto {

    private Long id;
    private Long subscriberId;
    private Long channelId;
}
