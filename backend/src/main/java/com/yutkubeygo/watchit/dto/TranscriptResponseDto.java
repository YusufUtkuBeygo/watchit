package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;
import java.util.List;

//GET /videos/{id}/transcript cevabı. status: NOT_STARTED, PENDING, PROCESSING, READY, FAILED.
//Cümleler yalnız READY iken dolu.
public record TranscriptResponseDto(
        Long videoId,
        String status,
        String language,
        String errorMessage,
        LocalDateTime updatedAt,
        List<Segment> segments) {

    public record Segment(double start, double end, String text) {}
}
