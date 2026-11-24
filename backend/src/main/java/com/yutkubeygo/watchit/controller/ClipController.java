package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ClipRequestDto;
import com.yutkubeygo.watchit.dto.ClipResponseDto;
import com.yutkubeygo.watchit.service.ClipService;
import com.yutkubeygo.watchit.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//Klipler. GET /clips/{id} herkese açık (SecurityConfig), gerisi giriş ister.
@RestController
@RequestMapping("/clips")
public class ClipController {

    private final ClipService clipService;
    private final EmailVerificationService emailVerificationService;

    public ClipController(ClipService clipService, EmailVerificationService emailVerificationService)
    {
        this.clipService = clipService;
        this.emailVerificationService = emailVerificationService;
    }

    //Klip oluşturmak içerik üretmek sayılır: yorum gibi doğrulanmış e-posta ister (403 EMAIL_NOT_VERIFIED)
    @PostMapping
    public ResponseEntity<ClipResponseDto> createClip(@AuthenticationPrincipal Long userId, @Valid @RequestBody ClipRequestDto request)
    {
        emailVerificationService.requireVerified(userId);
        ClipResponseDto clip = clipService.createClip(userId, request);
        if(clip == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(clip);
    }

    //Kliplerim
    @GetMapping
    public List<ClipResponseDto> getMyClips(@AuthenticationPrincipal Long userId)
    {
        return clipService.getMyClips(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClipResponseDto> getClip(@PathVariable Long id)
    {
        ClipResponseDto clip = clipService.getClip(id);
        if(clip == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(clip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClip(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        if(!clipService.deleteClip(id, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }
}
