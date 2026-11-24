package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;

//Kurslar sayfasındaki bir kurs: liste bilgisi + kaç ders var, kaçını izledim, ilk ders ve kapak
//watchedCount girişsiz istekte 0'dır
public record CourseResponseDto(
        Long id,
        String title,
        String description,
        String ownerName,
        int videoCount,
        long watchedCount,
        Long firstVideoId,
        String thumbnailUrl,
        LocalDateTime updatedAt
) {
}
