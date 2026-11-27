package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

//"Videoya sor" cevap önbelleği: aynı videoya aynı soru tekrar gelince Gemini'ye gitmeden önceki cevap verilir.
//Neden: ücretsiz kota günde ~1.000 soru; demo sırasında herkes aynı öneri sorularına basıyor.
//Anahtar transkriptin güncellenme zamanını içerir: video yeniden yazıya çevrilirse eski cevaplar kendiliğinden geçersiz olur.
//Bellekte, en fazla 1.000 cevap, her biri 24 saat (en eski kullanılan önce atılır: LRU)
@Component
public class AnswerCache {

    static final int MAX_ENTRIES = 1000;
    static final Duration TTL = Duration.ofHours(24);

    private record Entry(QuestionResponseDto answer, Instant storedAt) {}

    //accessOrder = true: get() kaydı sona taşır; removeEldestEntry en uzun süredir kullanılmayanı atar
    private final Map<String, Entry> entries = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest)
        {
            return size() > MAX_ENTRIES;
        }
    };

    public synchronized QuestionResponseDto get(Long videoId, LocalDateTime transcriptVersion, String question)
    {
        return get(videoId, transcriptVersion, question, Instant.now());
    }

    synchronized QuestionResponseDto get(Long videoId, LocalDateTime transcriptVersion, String question, Instant now)
    {
        String key = key(videoId, transcriptVersion, question);
        Entry entry = entries.get(key);
        if(entry == null)
            return null;
        if(entry.storedAt().plus(TTL).isBefore(now))
        {
            entries.remove(key);
            return null;
        }
        return entry.answer();
    }

    public synchronized void put(Long videoId, LocalDateTime transcriptVersion, String question, QuestionResponseDto answer)
    {
        entries.put(key(videoId, transcriptVersion, question), new Entry(answer, Instant.now()));
    }

    synchronized int size()
    {
        return entries.size();
    }

    //"Robot el KİMİN?  " ile "robot el kimin" aynı soru: küçük harf (Türkçe kurallarıyla), fazla boşluk ve sondaki noktalama atılır
    static String normalize(String question)
    {
        return question.toLowerCase(Locale.forLanguageTag("tr"))
                .replaceAll("\\s+", " ")
                .replaceAll("[\\s?!.,;:]+$", "")
                .trim();
    }

    private static String key(Long videoId, LocalDateTime transcriptVersion, String question)
    {
        return videoId + "|" + transcriptVersion + "|" + normalize(question);
    }
}
