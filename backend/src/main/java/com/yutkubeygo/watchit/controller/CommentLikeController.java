package com.yutkubeygo.watchit.controller;


import com.yutkubeygo.watchit.dto.CommentLikeRequestDto;
import com.yutkubeygo.watchit.dto.CommentLikeResponseDto;
import com.yutkubeygo.watchit.service.CommentLikeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<CommentLikeResponseDto> createCommentLike(@Valid @RequestBody  CommentLikeRequestDto commentLikeRequestDto,@AuthenticationPrincipal Long userId)
    {
        CommentLikeResponseDto commentLike = commentLikeService.createCommentLike(commentLikeRequestDto,userId);

        //Servis null döndüyse istekteki kullanıcı ya da yorum bulunamamıştır
        if(commentLike == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(commentLike);
    }

    @GetMapping
    public List<CommentLikeResponseDto> getAllCommentLikes()
    {
        return commentLikeService.getAllCommentLikes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentLikeResponseDto> getCommentLike(@PathVariable Long id)
    {
        CommentLikeResponseDto commentLike = commentLikeService.getCommentLikeById(id);

        if(commentLike == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(commentLike);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommentLikes(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        if(!commentLikeService.deleteCommentLike(id,userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
