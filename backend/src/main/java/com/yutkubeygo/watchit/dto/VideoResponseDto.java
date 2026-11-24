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
    //Çoklu kalite ana listesi (master.m3u8). Yalnız hazır ve şu anki dosyadan üretilmişse dolu; boşsa oynatıcı videoUrl'i (MP4) kullanır
    private String hlsUrl;
    //PENDING / PROCESSING / READY / FAILED; eski videolarda boş
    private String hlsStatus;
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
