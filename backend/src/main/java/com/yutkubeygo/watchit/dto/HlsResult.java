package com.yutkubeygo.watchit.dto;

import java.nio.file.Path;

//Medya servisinin ürettiği HLS paketi: bütün kaliteler ve master.m3u8 tek bir zip'te (geçici dosya, MinIO'ya
//açıldıktan sonra silinir). variants: üretilen kaliteler, ör. "720,480,360,240"
public record HlsResult(Path zip, String variants, double durationSeconds) {}
