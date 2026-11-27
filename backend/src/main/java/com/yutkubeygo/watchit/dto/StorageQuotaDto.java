package com.yutkubeygo.watchit.dto;

//GET /files/quota: kullanılan ve izin verilen alan (bayt). daily*: son 24 saatteki yüklemeler
public record StorageQuotaDto(long usedBytes, long totalBytes, long dailyUsedBytes, long dailyBytes, long maxFileBytes)
{
}
