package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.FeedbackRequestDto;
import com.yutkubeygo.watchit.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//"Geri bildirim gönder" (sol menü). Giriş ister (SecurityConfig: anyRequest().authenticated()).
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService)
    {
        this.feedbackService = feedbackService;
    }

    //201 kaydedildi, 400 boş/uzun metin, 429 saatlik sınır
    @PostMapping
    public ResponseEntity<Void> send(@AuthenticationPrincipal Long userId, @Valid @RequestBody FeedbackRequestDto request)
    {
        feedbackService.send(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
