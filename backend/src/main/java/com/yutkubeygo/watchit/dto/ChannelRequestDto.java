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
    @Size(max = 255)
    private String profilePictureUrl;
    @Size(max = 255)
    private String bannerImageUrl;

}
