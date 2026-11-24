package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

//POST /auth/verify-email gövdesi: maildeki 6 haneli kod
@Getter
@Setter
public class VerifyEmailRequestDto {
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
