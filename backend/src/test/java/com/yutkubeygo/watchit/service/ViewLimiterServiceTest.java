package com.yutkubeygo.watchit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

//ViewLimiterService kuralları: ilk izleme sayılır, bekleme süresi içinde tekrar sayılmaz, günde en fazla 20.
//Veritabanı ve Spring gerekmez: sınıf düz Java nesnesi olarak oluşturulur.
class ViewLimiterServiceTest {

    private ViewLimiterService limiter;

    @BeforeEach
    void setUp()
    {
        //Her test boş bir defterle başlar
        limiter = new ViewLimiterService();
    }

    @Test
    void firstViewIsCounted()
    {
        assertThat(limiter.tryCount("userId:5", 1L, 600)).isTrue();
    }

    @Test
    void repeatWithinCooldownIsNotCounted()
    {
        limiter.tryCount("userId:5", 1L, 600);

        //10 dakikalık video: bekleme süresi 5 dakika, hemen tekrar sayılmaz
        assertThat(limiter.tryCount("userId:5", 1L, 600)).isFalse();
    }

    @Test
    void differentViewersAreCountedSeparately()
    {
        limiter.tryCount("userId:5", 1L, 600);

        assertThat(limiter.tryCount("userId:6", 1L, 600)).isTrue();
        assertThat(limiter.tryCount("ip:1.2.3.4", 1L, 600)).isTrue();
    }

    @Test
    void differentVideosAreCountedSeparately()
    {
        limiter.tryCount("userId:5", 1L, 600);

        assertThat(limiter.tryCount("userId:5", 2L, 600)).isTrue();
    }

    @Test
    void dailyLimitIsTwenty()
    {
        //Süre 0: bekleme yok, yalnız tavan sınırlar
        for(int i = 1; i <= 20; i++)
            assertThat(limiter.tryCount("userId:5", 1L, 0)).as("%d. izleme", i).isTrue();

        assertThat(limiter.tryCount("userId:5", 1L, 0)).isFalse();
    }

    @Test
    void dailyLimitIsPerViewer()
    {
        for(int i = 0; i < 20; i++)
            limiter.tryCount("userId:5", 1L, 0);

        assertThat(limiter.tryCount("userId:6", 1L, 0)).isTrue();
    }

    @Test
    void missingDurationMeansNoCooldown()
    {
        limiter.tryCount("userId:5", 1L, null);

        //Süre bilinmiyorsa bekleme 0 sayılır, patlamaz
        assertThat(limiter.tryCount("userId:5", 1L, null)).isTrue();
    }
}
