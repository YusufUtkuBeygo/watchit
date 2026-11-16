package com.yutkubeygo.watchit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

//Whisper servisinin cevabı: tanınan dil ve zaman damgalı cümleler (fazla alanlar yok sayılır)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptionResult(String language, List<Segment> segments) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(double start, double end, String text) {}
}
