package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponseDto {

        private Long id;
        private String content;
        private Long videoId;
        private Long userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long likeCount;
        private Long parentCommentId;
        private boolean edited;

}
