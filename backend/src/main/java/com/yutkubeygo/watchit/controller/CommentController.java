package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
import org.springframework.data.domain.Sort;
import com.yutkubeygo.watchit.service.EmailVerificationService;

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
    //E-postası doğrulanmamış hesap bu işi yapamaz (403 EMAIL_NOT_VERIFIED)
    private final EmailVerificationService emailVerificationService;

    public CommentController(CommentService commentService, EmailVerificationService emailVerificationService) {
        this.commentService = commentService;
        this.emailVerificationService = emailVerificationService;
    }


    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long id, @AuthenticationPrincipal Long userId, @Valid @RequestBody CommentRequestDto commentRequestDto)
    {
        CommentResponseDto comment = commentService.updateComment(id,userId,commentRequestDto);

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
    //GET /comments?videoId=5&page=0&size=20: en yeni üst yorumlar + cevapları
    public ResponseEntity<PageResponseDto<CommentResponseDto>> getCommentsByVideoId(@RequestParam Long videoId,
                                                                                    @RequestParam(required = false) Integer page,
                                                                                    @RequestParam(required = false) Integer size)
    {
        PageResponseDto<CommentResponseDto> comments = commentService.getCommentsByVideoId(videoId,
                Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        if(comments == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@AuthenticationPrincipal Long userId, @Valid @RequestBody CommentRequestDto commentRequestDto)
    {
        emailVerificationService.requireVerified(userId);
        CommentResponseDto comment = commentService.createComment(userId,commentRequestDto);

        //Servis null döndüyse istekteki kullanıcı ya da video bulunamamıştır
        if(comment == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        if(!commentService.deleteComment(id,userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
