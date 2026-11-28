package com.yutkubeygo.watchit.ratelimit;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

//Hangi iş için ne kadar izin var. Her işin bir kısa (patlama) bir de uzun (günlük/saatlik) penceresi var:
//kısa pencere art arda spam'i, uzun pencere yavaş ama sürekli spam'i durdurur.
//Sayılar normal bir insanın asla takılmayacağı, botun hemen takılacağı yerde seçildi.
public enum RateLimit {

    COMMENT(new Window(Duration.ofMinutes(1), 10), new Window(Duration.ofDays(1), 200)),
    LIKE(new Window(Duration.ofMinutes(1), 60), new Window(Duration.ofDays(1), 1000)),
    SUBSCRIBE(new Window(Duration.ofMinutes(1), 30), new Window(Duration.ofDays(1), 300)),
    //Her video yüklemesi 2 adres ister (video + kapak)
    UPLOAD(new Window(Duration.ofHours(1), 30), new Window(Duration.ofDays(1), 100)),
    VIDEO_CREATE(new Window(Duration.ofHours(1), 15), new Window(Duration.ofDays(1), 50)),
    CLIP(new Window(Duration.ofMinutes(1), 10), new Window(Duration.ofDays(1), 100)),
    PLAYLIST(new Window(Duration.ofMinutes(1), 30), new Window(Duration.ofDays(1), 500)),
    REPORT(new Window(Duration.ofMinutes(1), 5), new Window(Duration.ofDays(1), 50)),
    //Kayıt girişsiz: IP adresine göre sayılır (aynı evdeki birkaç kişiyi engellemeyecek kadar geniş)
    REGISTER(new Window(Duration.ofHours(1), 5), new Window(Duration.ofDays(1), 20)),
    //Tarayıcı hata bildirimi: bozuk bir sayfa aynı hatayı art arda gönderebilir; tabloyu doldurmasın
    CLIENT_ERROR(new Window(Duration.ofMinutes(1), 10), new Window(Duration.ofDays(1), 100));

    public record Window(Duration duration, int max) {}

    private final List<Window> windows;
    private final Duration longest;

    RateLimit(Window... windows)
    {
        this.windows = List.of(windows);
        this.longest = Arrays.stream(windows).map(Window::duration).max(Duration::compareTo).orElseThrow();
    }

    public List<Window> windows()
    {
        return windows;
    }

    public Duration longestWindow()
    {
        return longest;
    }
}
