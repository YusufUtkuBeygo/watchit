package com.yutkubeygo.watchit.controller;


import com.yutkubeygo.watchit.dto.CommentLikeRequestDto;
import com.yutkubeygo.watchit.dto.CommentLikeResponseDto;
import com.yutkubeygo.watchit.service.CommentLikeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commentLikes")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;


    public CommentLikeController(CommentLikeService commentLikeService) {
        this.commentLikeService = commentLikeService;
    }

    @PostMapping
    public CommentLikeResponseDto createCommentLike(@RequestBody  CommentLikeRequestDto commentLikeRequestDto)
    {
        return commentLikeService.createCommentLike(commentLikeRequestDto);
    }

    @GetMapping
    public List<CommentLikeResponseDto> getAllCommentLikes()
    {
        return commentLikeService.getAllCommentLikes();
    }

    @GetMapping("/{id}")
    public CommentLikeResponseDto getCommentLike(@PathVariable Long id)
    {
        return commentLikeService.getCommentLikeById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteCommentLikes(@PathVariable Long id)
    {
        commentLikeService.deleteCommentLike(id);
    }

}
