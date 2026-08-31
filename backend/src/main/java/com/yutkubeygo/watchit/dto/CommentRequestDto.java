package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {

    private String content;
    private Long videoId;
    private Long userId;
    private Long parentCommentId;
}