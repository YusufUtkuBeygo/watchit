package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//Yeni klip: hangi video, adı ve aralığı. Uzunluk kuralı (5-60 sn) serviste, çünkü iki alana birden bakıyor.
@Getter
@Setter
public class ClipRequestDto {

    @NotNull
    private Long videoId;

    @NotBlank
    @Size(max = 140)
    private String title;

    @NotNull
    @Min(0)
    private Integer startSeconds;

    @NotNull
    @Min(0)
    private Integer endSeconds;
}
