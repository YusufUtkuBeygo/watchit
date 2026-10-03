package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {

    @NotBlank
    private String content;
    @NotNull
    private Long videoId;
    @NotNull
    private Long userId;
    private Long parentCommentId;
}