package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionResponseDto {

    private Long id;
    private Long subscriberId;
    private Long channelId;
    //Abonelik listesinde (sol menü) kanalı ayrıca istemeden göstermek için
    private String channelName;
    private String channelProfilePictureUrl;
}
