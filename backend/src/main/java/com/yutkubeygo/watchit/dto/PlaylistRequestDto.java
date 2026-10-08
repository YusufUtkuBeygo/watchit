package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistRequestDto {

    @NotBlank
    private String title;
    private String description;
    private Boolean isPublic;

}
