package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentLikeResponseDto {

    private Long id;
    private Long userId;
    private Long commentId;
    private LocalDateTime createdAt;

}
