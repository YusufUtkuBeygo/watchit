package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;

//Yönetim panelindeki şikayet satırı. İçerik bilgisi (başlık, yorum metni, sahibi) yöneticinin karar verebilmesi için eklenir;
//içerik silinmişse targetExists false ve içerik alanları boş
public record ReportResponseDto(
        Long id,
        String targetType,
        Long targetId,
        String reason,
        String details,
        String status,
        LocalDateTime createdAt,
        String reporterUsername,
        boolean targetExists,
        String targetText,
        Long targetVideoId,
        String targetOwnerUsername,
        long openReportsForTarget)
{
}
