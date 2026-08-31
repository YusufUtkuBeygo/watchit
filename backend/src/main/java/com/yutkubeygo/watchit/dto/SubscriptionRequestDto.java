package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequestDto {

    private Long subscriberId;
    private Long channelId;
}
