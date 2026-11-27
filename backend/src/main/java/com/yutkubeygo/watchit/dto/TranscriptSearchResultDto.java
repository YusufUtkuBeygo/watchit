package com.yutkubeygo.watchit.dto;

import java.util.List;

//"Videoların içinde geçenler": video + kelimenin kaç cümlede geçtiği + ilk birkaç geçtiği yer (saniye ve cümle)
public record TranscriptSearchResultDto(VideoResponseDto video, long hits, List<Match> matches)
{
    public record Match(double startSeconds, String text) {}
}
