package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.QuestionRequestDto;
import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import com.yutkubeygo.watchit.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//Videoya soru sor. Giriş ister (Gemini kotası kişi başına sınırlı).
//200 cevap (found=false: videoda yok), 400 boş/uzun soru, 401 giriş yok, 404 video yok,
//409 transkript hazır değil, 429 soru sınırı, 503 Gemini şu an cevap veremiyor.
@RestController
@RequestMapping("/videos/{id}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService)
    {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<QuestionResponseDto> ask(@PathVariable Long id, @Valid @RequestBody QuestionRequestDto request,
                                                   @AuthenticationPrincipal Long userId)
    {
        QuestionResponseDto answer = questionService.ask(id, userId, request.getQuestion());
        if(answer == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(answer);
    }
}
