package com.yutkubeygo.watchit.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Genel hız sınırı: "bu kişi bu işi son X sürede kaç kez yaptı" (kayan pencere).
//Neden: bir bot dakikada binlerce yorum/beğeni/kayıt atıp siteyi ve veritabanını doldurmasın.
//Bellekte tutulur (yeniden başlatınca sıfırlanır, tek sunucu için yeterli); çok sunucuda Redis gibi ortak bir yer gerekir.
@Service
public class RateLimiterService {

    //Anahtar: "COMMENT:user:5" ya da "REGISTER:ip:1.2.3.4" -> o işin son zamanları (eskiden yeniye)
    private final Map<String, Deque<Instant>> history = new ConcurrentHashMap<>();

    public boolean tryAcquire(RateLimit limit, String who)
    {
        return tryAcquire(limit, who, Instant.now());
    }

    //Zaman dışarıdan: testte "bir saat sonra" demek kolay olsun
    boolean tryAcquire(RateLimit limit, String who, Instant now)
    {
        Deque<Instant> times = history.computeIfAbsent(limit.name() + ":" + who, k -> new ArrayDeque<>());
        //Aynı kişinin aynı andaki iki isteği aynı listeyi değiştirmesin
        synchronized(times)
        {
            Duration longest = limit.longestWindow();
            while(!times.isEmpty() && !times.peekFirst().isAfter(now.minus(longest)))
                times.pollFirst();

            for(RateLimit.Window window : limit.windows())
            {
                Instant since = now.minus(window.duration());
                long count = times.stream().filter(t -> t.isAfter(since)).count();
                if(count >= window.max())
                    return false;
            }
            times.addLast(now);
            return true;
        }
    }

    //10 dakikada bir: süresi geçmiş kayıtları at, boş kalan anahtarları sil (bellek sonsuza kadar büyümesin)
    @Scheduled(fixedRate = 600_000)
    public void cleanUp()
    {
        cleanUp(Instant.now());
    }

    void cleanUp(Instant now)
    {
        history.entrySet().removeIf(entry -> {
            RateLimit limit = RateLimit.valueOf(entry.getKey().substring(0, entry.getKey().indexOf(':')));
            Deque<Instant> times = entry.getValue();
            synchronized(times)
            {
                while(!times.isEmpty() && !times.peekFirst().isAfter(now.minus(limit.longestWindow())))
                    times.pollFirst();
                return times.isEmpty();
            }
        });
    }

    int trackedKeys()
    {
        return history.size();
    }
}
