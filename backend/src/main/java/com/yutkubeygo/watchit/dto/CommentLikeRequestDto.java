package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentLikeRequestDto {

    @NotNull
    private Long userId;
    @NotNull
    private Long commentId;
}

