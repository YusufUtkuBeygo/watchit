package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelRequestDto {

    private String channelName;
    private String description;
    private String profilePictureUrl;
    private String bannerImageUrl;

    private Long ownerId; // şimdilik geçici authenticaton'dan sonra eklemey gerek yok req. atan kullanıcın idsi otomatik bulunur

}
