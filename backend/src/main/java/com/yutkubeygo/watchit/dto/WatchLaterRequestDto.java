package com.yutkubeygo.watchit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

//POST /watchLater gövdesi. Kullanıcı token'dan gelir
@Getter
@Setter
public class WatchLaterRequestDto {
    @NotNull
    private Long videoId;
}
