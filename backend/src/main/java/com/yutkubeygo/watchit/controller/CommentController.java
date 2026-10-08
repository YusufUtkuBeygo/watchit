package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long id, @Valid @RequestBody CommentRequestDto commentRequestDto)
    {
        CommentResponseDto comment = commentService.updateComment(id,commentRequestDto);

        //Servis null döndüyse yorum, kullanıcı, video ya da üst yorumdan biri bulunamamıştır
        if(comment == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(comment);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommentResponseDto> getComments(@PathVariable Long id)
    {
        CommentResponseDto comment = commentService.getCommentById(id);

        if(comment == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(comment);
    }

    @GetMapping
    public List<CommentResponseDto> getAllComments()
    {
        return commentService.getAllComments();
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@AuthenticationPrincipal Long userId, @Valid @RequestBody CommentRequestDto commentRequestDto)
    {
        CommentResponseDto comment = commentService.createComment(userId,commentRequestDto);

        //Servis null döndüyse istekteki kullanıcı ya da video bulunamamıştır
        if(comment == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id)
    {
        if(!commentService.deleteComment(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
