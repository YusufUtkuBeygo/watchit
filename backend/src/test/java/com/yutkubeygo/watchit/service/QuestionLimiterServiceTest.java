package com.yutkubeygo.watchit.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

//Soru sınırı: kullanıcı başına dakikada 5, günde 50. Zaman dışarıdan verilir, test beklemez.
class QuestionLimiterServiceTest {

    private final QuestionLimiterService limiter = new QuestionLimiterService();
    private final Instant now = Instant.parse("2026-11-16T12:00:00Z");

    @Test
    void fivePerMinuteThenBlocked()
    {
        for(int i = 0; i < 5; i++)
            assertThat(limiter.tryAcquire(1L, now.plusSeconds(i))).isTrue();

        assertThat(limiter.tryAcquire(1L, now.plusSeconds(10))).isFalse();
        assertThat(limiter.tryAcquire(1L, now.plusSeconds(61))).isTrue();   //ilk sorudan 1 dakika sonra yer açılır
    }

    @Test
    void usersAreIndependent()
    {
        for(int i = 0; i < 5; i++)
            limiter.tryAcquire(1L, now);

        assertThat(limiter.tryAcquire(2L, now)).isTrue();
    }

    @Test
    void fiftyPerDayThenBlockedUntilNextDay()
    {
        //Her 2 dakikada bir soru: dakika sınırına takılmaz, günlük sınıra takılır
        for(int i = 0; i < 50; i++)
            assertThat(limiter.tryAcquire(1L, now.plus(Duration.ofMinutes(2L * i)))).isTrue();

        Instant afterFifty = now.plus(Duration.ofMinutes(100));
        assertThat(limiter.tryAcquire(1L, afterFifty)).isFalse();
        assertThat(limiter.tryAcquire(1L, now.plus(Duration.ofDays(1)).plusSeconds(1))).isTrue();
    }
}
