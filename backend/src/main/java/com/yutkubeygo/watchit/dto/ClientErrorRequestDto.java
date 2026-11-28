package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//POST /client-errors gövdesi. Sınırlar geniş tutuldu, fazlası serviste kesilir (hata raporu reddedilmesin)
public record ClientErrorRequestDto(
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 20000) String stack,
        @Size(max = 2000) String pageUrl)
{
}
