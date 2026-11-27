package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerCacheTest {

    private final AnswerCache cache = new AnswerCache();
    private final LocalDateTime version = LocalDateTime.of(2026, 11, 27, 10, 0);
    private final QuestionResponseDto answer = new QuestionResponseDto(true, "Celia'nın", List.of(), QuestionResponseDto.VIDEO);

    @Test
    void sameQuestionWrittenDifferentlyHits()
    {
        cache.put(5L, version, "Robot el kimin?", answer);
        assertThat(cache.get(5L, version, "  robot   el KİMİN ")).isSameAs(answer);
        assertThat(cache.get(5L, version, "ROBOT EL KIMIN?")).isNull();   //Türkçe: I -> ı, İ -> i (farklı harf)
    }

    @Test
    void otherVideoOrNewTranscriptMisses()
    {
        cache.put(5L, version, "Robot el kimin?", answer);
        assertThat(cache.get(6L, version, "Robot el kimin?")).isNull();
        assertThat(cache.get(5L, version.plusMinutes(1), "Robot el kimin?")).isNull();
    }

    @Test
    void expiresAfter24Hours()
    {
        cache.put(5L, version, "soru", answer);
        assertThat(cache.get(5L, version, "soru", Instant.now().plus(AnswerCache.TTL).plusSeconds(5))).isNull();
        assertThat(cache.size()).isZero();
    }

    @Test
    void keepsAtMost1000()
    {
        for(int i = 0; i < AnswerCache.MAX_ENTRIES + 50; i++)
            cache.put((long) i, version, "soru", answer);
        assertThat(cache.size()).isEqualTo(AnswerCache.MAX_ENTRIES);
        assertThat(cache.get(0L, version, "soru")).isNull();          //en eski atıldı
        assertThat(cache.get(1049L, version, "soru")).isSameAs(answer);
    }
}
