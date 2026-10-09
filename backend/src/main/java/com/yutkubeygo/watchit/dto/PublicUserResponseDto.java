package com.yutkubeygo.watchit.dto;


import lombok.Getter;
import lombok.Setter;

//Başka bir kullanıcıya gösterilen bilgiler: e-posta yok (kendi bilgilerim için UserResponseDto)
@Getter
@Setter
public class PublicUserResponseDto {

    private Long id;
    private String username;


}
