package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDto {
    //id alanı yok: id'yi veritabanı verir. Eskiden istekteki id ile var olan kategorinin üzerine yazılabiliyordu
    @NotBlank
    @Size(max = 255)
    private String name;
}
