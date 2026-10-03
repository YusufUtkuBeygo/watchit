package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelRequestDto {

    @NotBlank
    private String channelName;
    private String description;
    private String profilePictureUrl;
    private String bannerImageUrl;

    @NotNull
    private Long ownerId; // şimdilik geçici authenticaton'dan sonra eklemey gerek yok req. atan kullanıcın idsi otomatik bulunur

}
