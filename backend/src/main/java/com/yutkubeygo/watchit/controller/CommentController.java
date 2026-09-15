package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    @PutMapping("/{id}")
    public CommentResponseDto updateComment(@PathVariable Long id, @RequestBody CommentRequestDto commentRequestDto)
    {
        return commentService.updateComment(id,commentRequestDto);
    }


    @GetMapping("/{id}")
    public CommentResponseDto getComments(@PathVariable Long id)
    {
        return commentService.getCommentById(id);
    }

    @GetMapping
    public List<CommentResponseDto> getAllComments()
    {
        return commentService.getAllComments();
    }

    @PostMapping
    public CommentResponseDto createComment(@RequestBody CommentRequestDto commentRequestDto)
    {
        return commentService.createComment(commentRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Long id)
    {
        commentService.deleteComment(id);
    }
}
