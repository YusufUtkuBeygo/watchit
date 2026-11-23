package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VideoResponseDto {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;
    //PENDING / PROCESSING / READY / FAILED; eski videolarda null
    private String processingStatus;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Long viewCount;

    private Long channelId;
    //Kart ve listelerde kanal adını göstermek için (ayrı istek atmadan)
    private String channelName;
    private String channelProfilePictureUrl;
    private Long categoryId;
    private boolean likedByMe;
    //Giriş yapanın "Daha sonra izle" listesinde mi (yalnız tek video cevabında dolar)
    private boolean savedForLater;
}
