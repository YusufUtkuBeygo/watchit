package com.yutkubeygo.watchit.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Kullanıcı başına soru sınırı: dakikada 5, günde 50. Gemini'nin ücretsiz kotası projeye bağlı;
//tek bir kullanıcı herkesin kotasını bitiremesin. Bellekte tutulur (yeniden başlatınca sıfırlanır), görüntülenme sınırlayıcısı gibi.
@Service
public class QuestionLimiterService {

    static final int PER_MINUTE = 5;
    static final int PER_DAY = 50;

    //Kullanıcının son 24 saatteki sorularının zamanları (eskiden yeniye)
    private final Map<Long, Deque<Instant>> history = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId)
    {
        return tryAcquire(userId, Instant.now());
    }

    //Zamanı dışarıdan almak testte "1 dakika sonra" demeyi kolaylaştırır
    synchronized boolean tryAcquire(Long userId, Instant now)
    {
        Deque<Instant> times = history.computeIfAbsent(userId, id -> new ArrayDeque<>());
        while(!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofDays(1))))
            times.pollFirst();

        long lastMinute = times.stream().filter(t -> t.isAfter(now.minus(Duration.ofMinutes(1)))).count();
        if(lastMinute >= PER_MINUTE || times.size() >= PER_DAY)
            return false;

        times.addLast(now);
        return true;
    }
}
