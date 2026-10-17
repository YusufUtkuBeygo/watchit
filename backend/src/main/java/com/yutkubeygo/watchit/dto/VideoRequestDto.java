package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoRequestDto {
    @NotBlank
    @Size(max = 255)
    private String title;
    @Size(max = 5000)
    private String description;
    @Size(max = 255)
    private String thumbnailUrl;
    @Size(max = 255)
    private String videoUrl;
    @Min(0)
    private Integer durationInSeconds;

    //Kanal gövdeden gelmez: servis kullanıcının tek kanalını token'daki id ile bulur
    @NotNull
    private Long categoryId;
}
