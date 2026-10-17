package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistRequestDto {

    @NotBlank
    @Size(max = 255)
    private String title;
    @Size(max = 255)
    private String description;
    private Boolean isPublic;

}
