package com.yutkubeygo.watchit.dto;

//GET /videos/{id}/chapters satırı
public record ChapterDto(int startSeconds, String title)
{
}
