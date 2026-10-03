package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDto {
    Long id;
    @NotBlank
    String name;
}
