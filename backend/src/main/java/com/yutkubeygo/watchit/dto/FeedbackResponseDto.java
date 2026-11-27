package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;

//Yönetim panelinde geri bildirim satırı (yazan hesabını sildiyse username boş)
public record FeedbackResponseDto(Long id, String username, String message, String pageUrl, LocalDateTime createdAt)
{
}
