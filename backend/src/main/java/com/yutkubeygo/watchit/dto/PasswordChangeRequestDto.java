package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//PUT /users/me/password: token'ı ele geçiren biri şifreyi değiştiremesin diye şu anki şifre zorunlu
@Getter
@Setter
public class PasswordChangeRequestDto {

    @NotBlank
    @Size(max = 72)
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
