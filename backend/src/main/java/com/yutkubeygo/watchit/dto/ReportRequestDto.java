package com.yutkubeygo.watchit.dto;

import com.yutkubeygo.watchit.enums.ReportReason;
import com.yutkubeygo.watchit.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//POST /reports gövdesi: { "targetType": "VIDEO", "targetId": 5, "reason": "SPAM", "details": "..." }
//Şikayet eden token'dan gelir
public record ReportRequestDto(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ReportReason reason,
        @Size(max = 1000) String details)
{
}
