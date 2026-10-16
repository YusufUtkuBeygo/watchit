package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelRequestDto {

    @NotBlank
    @Size(max = 255)
    private String channelName;
    @Size(max = 5000)
    private String description;
    private String profilePictureUrl;
    private String bannerImageUrl;

}
