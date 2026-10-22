package com.yutkubeygo.watchit.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ViewLimiterService {
    private static final int DAILY_LIMIT=20;

    //Defterdeki bir satır:son sayılma anı , bugünkü sayım , hangi gün
    private record ViewRecord(Instant lastCounted, int countToday, LocalDate day){}

    //Defter. Anahtar "izleyici:videoId" , ör:"user:5:12"
    private final Map<String,ViewRecord> record = new ConcurrentHashMap<>();

     public synchronized boolean tryCount (String viewer , Long videoId , Integer durationInSeconds)
     {
         String key = viewer +":"+videoId;
         Instant now = Instant.now();
         LocalDate today = LocalDate.now();
         ViewRecord old = record.get(key);

         //1)İlk izleme: deftere yaz, say
         if(old == null)
         {
             record.put(key,new ViewRecord(now,1,today));
             return true;
         }

         //2 Bekleme süresi:videonun yarısı (2x hızda izleyen de sayılsın)
         int duration = (durationInSeconds == null) ? 0:durationInSeconds;
         Duration cooldown = Duration.ofSeconds(duration/2);
         if(now.isBefore(old.lastCounted().plus(cooldown)))
            return false;

         //3) Yeni gün:sayım baştan başlar
         if(!old.day().equals(today))
         {
             record.put(key,new ViewRecord(now,1,today));
             return true;
         }

         //4)Günlük limit aşıldı
             if(old.countToday()>= DAILY_LIMIT)
                 return false;

             //5)Kontrollerin hepsini geçti:sayımı arttırıp yaz
                record.put(key, new ViewRecord(now,old.countToday()+1,today));
                return true;

     }
}
