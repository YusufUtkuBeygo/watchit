package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistRequestDto {

    private String title;
    private String description;
    private Boolean isPublic;
    private Long ownerId;

}
