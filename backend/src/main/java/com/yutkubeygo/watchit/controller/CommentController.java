package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
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
    //GET /comments?videoId=5&sort=new|top&page=0&size=20: üst yorumlar + cevapları (sabitlenmiş ilk sayfada en üstte)
    //Açık uç; giriş yapılmışsa her yorumda likedByMe dolar
    public ResponseEntity<PageResponseDto<CommentResponseDto>> getCommentsByVideoId(@RequestParam Long videoId,
                                                                                    @RequestParam(required = false) String sort,
                                                                                    @RequestParam(required = false) Integer page,
                                                                                    @RequestParam(required = false) Integer size,
                                                                                    @AuthenticationPrincipal Long userId)
    {
        if(sort != null && !sort.equals("new") && !sort.equals("top"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        PageResponseDto<CommentResponseDto> comments = commentService.getCommentsByVideoId(videoId, sort, userId,
                Paging.of(page, size));
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

    //Video sahibi yorumu sabitler: PUT /comments/{id}/pin ; kaldırır: DELETE /comments/pin?videoId=5
    //204, 404 (yorum/video yok), 403 (video sahibi değil), 400 (cevap yorumu sabitlenemez)
    @PutMapping("/{id}/pin")
    public ResponseEntity<Void> pin(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        CommentResponseDto comment = commentService.getCommentById(id);
        if(comment == null || !commentService.pinComment(comment.getVideoId(), id, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pin")
    public ResponseEntity<Void> unpin(@RequestParam Long videoId, @AuthenticationPrincipal Long userId)
    {
        if(!commentService.pinComment(videoId, null, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }
}
