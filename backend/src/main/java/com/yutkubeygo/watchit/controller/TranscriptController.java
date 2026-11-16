package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.TranscriptResponseDto;
import com.yutkubeygo.watchit.service.TranscriptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//Videonun transkripti. GET herkese açık (videolar gibi), POST giriş ister ve yalnız video sahibi yapabilir.
@RestController
@RequestMapping("/videos/{id}/transcript")
public class TranscriptController {

    private final TranscriptService transcriptService;

    public TranscriptController(TranscriptService transcriptService)
    {
        this.transcriptService = transcriptService;
    }

    //Durum + (hazırsa) zaman damgalı cümleler; video yoksa 404
    @GetMapping
    public ResponseEntity<TranscriptResponseDto> getTranscript(@PathVariable Long id)
    {
        TranscriptResponseDto transcript = transcriptService.getTranscript(id);
        if(transcript == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(transcript);
    }

    //Transkripti (yeniden) başlatır: 202 (sıraya alındı), 403 (sahibi değil), 404 (video yok), 409 (zaten işleniyor)
    @PostMapping
    public ResponseEntity<TranscriptResponseDto> requestTranscript(@PathVariable Long id, @AuthenticationPrincipal Long userId)
    {
        TranscriptResponseDto transcript = transcriptService.requestTranscript(id, userId);
        if(transcript == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transcript);
    }
}
