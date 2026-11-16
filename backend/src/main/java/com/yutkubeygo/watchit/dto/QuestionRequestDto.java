package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//POST /videos/{id}/questions gövdesi
@Getter
@Setter
public class QuestionRequestDto {

    @NotBlank
    @Size(max = 500)
    private String question;
}
