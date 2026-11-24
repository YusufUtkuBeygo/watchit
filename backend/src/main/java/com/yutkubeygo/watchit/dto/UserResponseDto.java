package com.yutkubeygo.watchit.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    //Frontend "e-postanı doğrula" uyarısını buna göre gösterir
    private boolean emailVerified;


}
