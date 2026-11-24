package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//Geri bildirim: metin zorunlu, hangi sayfada yazıldığı isteğe bağlı
@Getter
@Setter
public class FeedbackRequestDto {

    @NotBlank
    @Size(max = 2000)
    private String message;

    @Size(max = 255)
    private String pageUrl;
}
