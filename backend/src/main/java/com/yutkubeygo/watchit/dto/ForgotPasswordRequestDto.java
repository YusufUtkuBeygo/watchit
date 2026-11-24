package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//POST /auth/forgot-password gövdesi
@Getter
@Setter
public class ForgotPasswordRequestDto {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
