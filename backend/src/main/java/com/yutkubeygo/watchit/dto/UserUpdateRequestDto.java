package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//PUT /users/me: yalnız kullanıcı adı ve e-posta değişir, şifre ayrı uçtan (PUT /users/me/password).
//E-posta giriş bilgisi olduğu için değiştirilirken şu anki şifre istenir; yalnız ad değişiyorsa gerekmez.
@Getter
@Setter
public class UserUpdateRequestDto {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 72)
    private String currentPassword;
}
