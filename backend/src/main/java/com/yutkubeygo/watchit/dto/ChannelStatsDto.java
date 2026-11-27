package com.yutkubeygo.watchit.dto;

import java.time.LocalDate;
import java.util.List;

//Stüdyo istatistikleri: toplamlar + son N günün günlük görüntülenmesi (izlenme olmayan günler 0 olarak dahil, grafik boşluksuz çizilsin)
public record ChannelStatsDto(long totalViews, long subscriberCount, long videoCount, long periodViews, List<Day> daily)
{
    public record Day(LocalDate date, long views) {}
}
