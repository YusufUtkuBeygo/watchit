package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
//Owner bilgisi yok gereksiz bilgiere owner üzerinden erişilmesini engellemek için
//Lazım olursa ileride bu dto'nun içine koymak için bir summaryUserDto sınıfı oluşturabiliriz
public class ChannelResponseDto {

    private Long id;
    private String channelName;
    private String description;

    private Long subscriberCount;
    private Long videoCount;
    private Long totalViews;

    private String profilePictureUrl;
    private String bannerImageUrl;

    private Boolean verified;
    private String handle;
    private LocalDateTime createdAt;

}
