package com.yutkubeygo.watchit.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private final RateLimiterService limiter = new RateLimiterService();
    private final Instant start = Instant.parse("2026-11-27T10:00:00Z");

    @Test
    void shortWindowBlocksBurstAndReopensAfterAMinute()
    {
        for(int i = 0; i < 10; i++)
            assertThat(limiter.tryAcquire(RateLimit.COMMENT, "user:1", start.plusSeconds(i))).isTrue();
        //11. yorum aynı dakika içinde: hayır
        assertThat(limiter.tryAcquire(RateLimit.COMMENT, "user:1", start.plusSeconds(30))).isFalse();
        //Bir dakika sonra yeniden açılır
        assertThat(limiter.tryAcquire(RateLimit.COMMENT, "user:1", start.plusSeconds(61))).isTrue();
    }

    @Test
    void longWindowBlocksSlowSpam()
    {
        //Saatte 5 kayıt sınırı (aynı IP): dakikada bir de olsa 6. reddedilir
        for(int i = 0; i < 5; i++)
            assertThat(limiter.tryAcquire(RateLimit.REGISTER, "ip:1.2.3.4", start.plus(Duration.ofMinutes(i * 10)))).isTrue();
        assertThat(limiter.tryAcquire(RateLimit.REGISTER, "ip:1.2.3.4", start.plus(Duration.ofMinutes(55)))).isFalse();
        assertThat(limiter.tryAcquire(RateLimit.REGISTER, "ip:1.2.3.4", start.plus(Duration.ofMinutes(61)))).isTrue();
    }

    @Test
    void usersAndActionsAreCountedSeparately()
    {
        for(int i = 0; i < 10; i++)
            limiter.tryAcquire(RateLimit.COMMENT, "user:1", start);
        assertThat(limiter.tryAcquire(RateLimit.COMMENT, "user:1", start)).isFalse();
        assertThat(limiter.tryAcquire(RateLimit.COMMENT, "user:2", start)).isTrue();
        assertThat(limiter.tryAcquire(RateLimit.LIKE, "user:1", start)).isTrue();
    }

    @Test
    void cleanUpDropsExpiredKeys()
    {
        limiter.tryAcquire(RateLimit.COMMENT, "user:1", start);
        limiter.tryAcquire(RateLimit.REGISTER, "ip:1.2.3.4", start);
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        //Yorumun en uzun penceresi 1 gün, kaydın da 1 gün: 25 saat sonra ikisi de silinir
        limiter.cleanUp(start.plus(Duration.ofHours(25)));
        assertThat(limiter.trackedKeys()).isZero();
    }
}
