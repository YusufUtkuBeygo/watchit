package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
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

}
