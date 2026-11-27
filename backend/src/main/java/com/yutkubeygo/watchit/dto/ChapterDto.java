package com.yutkubeygo.watchit.dto;

//GET /videos/{id}/chapters satırı. thumbnailUrl: bölümün videodan bir karesi (yoksa null)
public record ChapterDto(int startSeconds, String title, String thumbnailUrl)
{
    //Gemini'nin ürettiği bölüm: henüz karesi yok
    public ChapterDto(int startSeconds, String title)
    {
        this(startSeconds, title, null);
    }
}
