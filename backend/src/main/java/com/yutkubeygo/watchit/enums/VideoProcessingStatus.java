package com.yutkubeygo.watchit.enums;

//Yüklenen videonun dönüştürülme durumu (en fazla 720p, H.264 + AAC, MP4)
//PENDING: sırada, PROCESSING: dönüştürülüyor, READY: dönüşmüş dosya yayında, FAILED: dönüştürülemedi (orijinal oynar)
public enum VideoProcessingStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
