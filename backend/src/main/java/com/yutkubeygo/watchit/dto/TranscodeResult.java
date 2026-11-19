package com.yutkubeygo.watchit.dto;

import java.nio.file.Path;

//Medya servisinin dönüştürdüğü dosya (geçici dosya, MinIO'ya yazıldıktan sonra silinir)
//mode: "copy" (zaten uygundu, yalnız yeniden paketlendi) ya da "transcode" (yeniden kodlandı)
public record TranscodeResult(Path file, String mode, double durationSeconds, Integer width, Integer height) {}
