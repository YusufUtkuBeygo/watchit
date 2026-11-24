package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;

//Klip cevabı: klibin bilgisi + kaynak videonun kartta gereken bilgileri (video, VideoResponseDto olarak)
public record ClipResponseDto(
        Long id,
        String title,
        Integer startSeconds,
        Integer endSeconds,
        LocalDateTime createdAt,
        Long userId,
        String username,
        VideoResponseDto video
) {
}
