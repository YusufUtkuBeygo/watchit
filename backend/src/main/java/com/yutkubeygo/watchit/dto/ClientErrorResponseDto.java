package com.yutkubeygo.watchit.dto;

import java.time.LocalDateTime;

//Yönetim panelindeki hata satırı
public record ClientErrorResponseDto(Long id, String message, String stack, String pageUrl, String userAgent,
                                     Long userId, LocalDateTime createdAt)
{
}
