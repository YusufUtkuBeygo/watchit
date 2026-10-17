package com.yutkubeygo.watchit.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Şifre denemesini sınırlar: aynı e-postaya 15 dakika içinde 5 yanlış denemeden sonra 15 dakika giriş kapanır (429).
//Bilgiler bellekte tutulur: uygulama yeniden başlayınca sıfırlanır, tek sunucu için yeterli.
//Bilinen bedel: biri başkasının e-postasıyla bilerek yanlış deneme yaparsa o hesap 15 dakika kilitlenir.
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    //Bir e-postanın yanlış deneme sayısı ve ilk yanlış denemenin zamanı
    private record Attempts(int failures, Instant firstFailure) {}

    //ConcurrentHashMap: aynı anda gelen isteklerin birbirinin kaydını bozmaması için
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email)
    {
        Attempts a = attempts.get(email);
        if(a == null)
            return false;

        //Süre dolduysa kaydı sil, tekrar denenebilir
        if(Instant.now().isAfter(a.firstFailure().plus(WINDOW)))
        {
            attempts.remove(email);
            return false;
        }
        return a.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String email)
    {
        //merge: kayıt yoksa ilk denemeyi yazar, varsa sayıyı bir artırır (süresi dolmuşsa baştan başlar)
        attempts.merge(email, new Attempts(1, Instant.now()), (old, first) ->
                Instant.now().isAfter(old.firstFailure().plus(WINDOW))
                        ? first
                        : new Attempts(old.failures() + 1, old.firstFailure()));
    }

    //Doğru girişte geçmiş yanlış denemeler silinir
    public void recordSuccess(String email)
    {
        attempts.remove(email);
    }
}
