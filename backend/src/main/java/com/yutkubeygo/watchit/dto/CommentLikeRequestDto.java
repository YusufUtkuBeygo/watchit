package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentLikeRequestDto {

    private Long userId;
    private Long commentId;
}

