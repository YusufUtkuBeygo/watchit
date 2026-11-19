package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import com.yutkubeygo.watchit.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

//"Videoya soru sor" değerlendirmesi (2f): src/test/resources/eval/sorular.json'daki 30 soruyu gerçek sisteme sorar.
//Gerçek Gemini ve yerel veritabanı gerekir (videolar ve transkriptleri READY olmalı), bu yüzden normal testlerde çalışmaz:
//    ./gradlew eval
//Ölçülenler:
//  Hit@3      cevaplı sorularda, sistemin kullandığı ilk 3 parçadan biri doğru zaman aralığıyla örtüşüyor mu
//  Doğruluk   cevaplı sorularda cevap bulundu mu ve beklenen kelimeler geçiyor mu
//  Bulunamadı cevapsız sorularda sistem "bulunamadı" diyor mu
//Sonuç: build/eval/sonuc.json (her soru ayrıntılı) ve build/eval/sonuc.md (tablo).
@SpringBootTest
@Tag("eval")
class QaEvaluation {

    @Autowired private TranscriptRetriever retriever;
    @Autowired private QuestionService questionService;
    @Autowired private JdbcTemplate jdbcTemplate;

    //Dakikada 5 soru sınırı değerlendirmeyi yavaşlatmasın; Gemini'ye yüklenmemek için sorular arasında yine bekliyoruz
    @MockitoBean
    private QuestionLimiterService limiter;

    private static final long PAUSE_MS = Long.parseLong(System.getProperty("eval.pauseMs", "4000"));

    @Test
    void evaluate() throws Exception
    {
        when(limiter.tryAcquire(any())).thenReturn(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try(InputStream in = getClass().getResourceAsStream("/eval/sorular.json"))
        {
            root = mapper.readTree(in);
        }

        Map<String, Long> videoIds = new HashMap<>();
        for(Map.Entry<String, JsonNode> e : root.get("videolar").properties())
            videoIds.put(e.getKey(), findVideoId(e.getValue().asString()));

        List<Map<String, Object>> results = new ArrayList<>();
        for(JsonNode q : root.get("sorular"))
        {
            results.add(evaluateOne(q, videoIds.get(q.get("video").asString())));
            Thread.sleep(PAUSE_MS);
        }

        Map<String, Object> metrics = metrics(results);
        Path dir = Path.of("build", "eval");
        Files.createDirectories(dir);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrikler", metrics);
        out.put("minScore", retriever.minScore());
        out.put("sorular", results);
        Files.writeString(dir.resolve("sonuc.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
        Files.writeString(dir.resolve("sonuc.md"), markdown(metrics, results));

        System.out.println("\n=== Değerlendirme ===");
        metrics.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("Ayrıntı: " + dir.toAbsolutePath().resolve("sonuc.md"));
    }

    private Map<String, Object> evaluateOne(JsonNode q, Long videoId) throws InterruptedException
    {
        String id = q.get("id").asString();
        String question = q.get("question").asString();
        boolean answerable = q.get("answerable").asBoolean();
        List<double[]> gold = new ArrayList<>();
        for(JsonNode g : q.get("gold"))
            gold.add(new double[]{g.get(0).asDouble(), g.get(1).asDouble()});

        //1) Parça seçimi: QuestionService'in yaptığının aynısı (tipine göre zaman, özet ya da anlam)
        QuestionIntent intent = QuestionIntent.of(question);
        boolean hasSummary = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT summary IS NOT NULL FROM video_transcripts WHERE video_id = ?", Boolean.class, videoId));
        String mode;
        List<QuestionResponseDto.Source> used;
        List<QuestionResponseDto.Source> nearest = List.of();
        switch(intent.type())
        {
            case SUMMARY ->
            {
                mode = hasSummary ? "özet" : "anlam";
                if(hasSummary)
                    used = List.of();
                else
                {
                    nearest = retriever.findNearest(videoId, question);
                    used = aboveThreshold(nearest);
                }
            }
            case BEGINNING, END, AT_TIME ->
            {
                mode = "zaman";
                used = retriever.findByTime(videoId, intent);
            }
            default ->
            {
                mode = "anlam";
                nearest = retriever.findNearest(videoId, question);
                used = aboveThreshold(nearest);
            }
        }
        Boolean hit = gold.isEmpty() ? null : overlapsTop3(used, gold);
        Boolean hit1 = gold.isEmpty() ? null : !used.isEmpty() && overlaps(used.getFirst(), gold);
        Double randomHit3 = gold.isEmpty() ? null : randomHit3(videoId, gold);
        //Eşik olmasaydı doğru parça ilk 3'te miydi (eşiğin cevaplı soruları kesip kesmediğini görmek için)
        Boolean hitWithoutThreshold = gold.isEmpty() || nearest.isEmpty() ? null : overlapsTop3(nearest, gold);

        //2) Uçtan uca cevap (gerçek QuestionService; Gemini meşgulse iki kez daha dener)
        QuestionResponseDto response = null;
        String error = null;
        long started = System.currentTimeMillis();
        for(int attempt = 0; attempt < 3 && response == null; attempt++)
        {
            try
            {
                response = questionService.ask(videoId, 0L, question);
            }
            catch(ServiceUnavailableException e)
            {
                error = "503";
                Thread.sleep(20_000);
            }
        }
        long millis = System.currentTimeMillis() - started;

        boolean found = response != null && response.found();
        String answer = response == null ? null : response.answer();
        boolean keywordsOk = answer != null && keywordsMatch(answer, q.get("keywords"));
        boolean correct = response != null && (answerable ? found && keywordsOk : !found);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("video", q.get("video").asString());
        r.put("soru", question);
        r.put("cevapli", answerable);
        r.put("tip", intent.type().name());
        r.put("secim", mode);
        r.put("enYuksekSkor", nearest.isEmpty() ? null : nearest.getFirst().score());
        r.put("parcalar", used.stream().map(s -> fmt(s.start()) + "-" + fmt(s.end()) + (s.score() == null ? "" : " (" + s.score() + ")")).toList());
        r.put("hit1", hit1);
        r.put("hit3", hit);
        r.put("rastgeleHit3", randomHit3);
        r.put("hit3Esiksiz", hitWithoutThreshold);
        r.put("bulundu", found);
        r.put("cevap", answer);
        r.put("anahtarKelime", keywordsOk);
        r.put("dogru", correct);
        r.put("sureMs", millis);
        r.put("hata", response == null ? error : null);
        r.put("beklenen", q.get("reference").asString());
        System.out.printf("%-4s %-5s hit=%-5s bulundu=%-5s doğru=%-5s %s%n", id, mode, hit, found, correct, answer);
        return r;
    }

    private List<QuestionResponseDto.Source> aboveThreshold(List<QuestionResponseDto.Source> sources)
    {
        return sources.stream().filter(s -> s.score() >= retriever.minScore()).toList();
    }

    private static boolean overlapsTop3(List<QuestionResponseDto.Source> sources, List<double[]> gold)
    {
        return sources.stream().limit(3).anyMatch(s -> overlaps(s, gold));
    }

    private static boolean overlaps(QuestionResponseDto.Source s, List<double[]> gold)
    {
        return gold.stream().anyMatch(g -> s.start() < g[1] && s.end() > g[0]);
    }

    //Taban çizgisi: videonun parçalarından rastgele 3 tanesini seçseydik doğru parçanın gelme olasılığı.
    //Kısa videoda parça az olduğu için bu yüksek çıkar; Hit@3'ü bununla birlikte okumak gerekir.
    private double randomHit3(Long videoId, List<double[]> gold)
    {
        List<double[]> chunks = jdbcTemplate.query(
                "SELECT (metadata->>'start')::float8 AS s, (metadata->>'end')::float8 AS e FROM vector_store WHERE video_id = ?",
                (rs, i) -> new double[]{rs.getDouble("s"), rs.getDouble("e")}, videoId);
        int n = chunks.size();
        long good = chunks.stream().filter(c -> gold.stream().anyMatch(g -> c[0] < g[1] && c[1] > g[0])).count();
        if(n <= 3)
            return good > 0 ? 1.0 : 0.0;
        //1 - (hiçbiri doğru değil): C(n-good, 3) / C(n, 3)
        double none = 1.0;
        for(int i = 0; i < 3; i++)
            none *= (double) (n - good - i) / (n - i);
        return 1 - Math.max(0, none);
    }

    //Her grup için seçeneklerden biri cevapta geçmeli. Cevaptaki "(03:18)" gibi zamanlar silinir: "10" yanlışlıkla eşleşmesin.
    static boolean keywordsMatch(String answer, JsonNode groups)
    {
        String text = normalize(answer.replaceAll("\\d{1,2}:\\d{2}(:\\d{2})?", " "));
        for(JsonNode group : groups)
        {
            boolean any = false;
            for(JsonNode option : group)
                any |= text.contains(normalize(option.asString()));
            if(!any)
                return false;
        }
        return true;
    }

    //Küçük harf + Türkçe işaretsiz: "Dünya" -> "dunya", "İstasyon" -> "istasyon"
    static String normalize(String s)
    {
        String lower = s.toLowerCase(Locale.forLanguageTag("tr"));
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('ı', 'i');
    }

    private Long findVideoId(String title)
    {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM videos WHERE title = ? ORDER BY id", Long.class, title);
        if(ids.isEmpty())
            throw new IllegalStateException("Değerlendirme videosu veritabanında yok: " + title);
        return ids.getFirst();
    }

    private static Map<String, Object> metrics(List<Map<String, Object>> results)
    {
        List<Map<String, Object>> answerable = results.stream().filter(r -> (boolean) r.get("cevapli")).toList();
        List<Map<String, Object>> unanswerable = results.stream().filter(r -> !(boolean) r.get("cevapli")).toList();
        List<Map<String, Object>> withGold = answerable.stream().filter(r -> r.get("hit3") != null).toList();

        long hits = withGold.stream().filter(r -> (boolean) r.get("hit3")).count();
        long hits1 = withGold.stream().filter(r -> (boolean) r.get("hit1")).count();
        double randomAvg = withGold.stream().mapToDouble(r -> (double) r.get("rastgeleHit3")).average().orElse(0);
        //Eşiğin işe yarayıp yaramadığı: cevaplı ve cevapsız soruların en yüksek benzerlik puanları
        List<Double> answerableTop = answerable.stream().map(r -> (Double) r.get("enYuksekSkor")).filter(s -> s != null).sorted().toList();
        List<Double> unanswerableTop = unanswerable.stream().map(r -> (Double) r.get("enYuksekSkor")).filter(s -> s != null).sorted().toList();
        long correct = answerable.stream().filter(r -> (boolean) r.get("dogru")).count();
        long notFound = unanswerable.stream().filter(r -> (boolean) r.get("dogru")).count();
        long missed = answerable.stream().filter(r -> !(boolean) r.get("bulundu")).count();
        long errors = results.stream().filter(r -> r.get("hata") != null).count();
        double avgMs = results.stream().mapToLong(r -> (long) r.get("sureMs")).average().orElse(0);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Hit@3", ratio(hits, withGold.size()) + " (hedef >= %80)");
        m.put("Hit@3 rastgele taban", "%" + Math.round(randomAvg * 100));
        m.put("Hit@1", ratio(hits1, withGold.size()));
        m.put("En yüksek puan, cevaplı", range(answerableTop));
        m.put("En yüksek puan, cevapsız", range(unanswerableTop));
        m.put("Doğruluk", ratio(correct, answerable.size()) + " (hedef >= %75)");
        m.put("Bulunamadı", ratio(notFound, unanswerable.size()) + " (hedef >= %90)");
        m.put("Cevaplı soruya yanlışlıkla 'bulunamadı'", missed + "/" + answerable.size());
        m.put("Hata (503)", errors);
        m.put("Ortalama süre", Math.round(avgMs) + " ms");
        return m;
    }

    private static String range(List<Double> sorted)
    {
        return sorted.isEmpty() ? "-" : sorted.getFirst() + " - " + sorted.getLast() + " (" + sorted.size() + " soru)";
    }

    private static String ratio(long part, long whole)
    {
        return part + "/" + whole + " = %" + (whole == 0 ? 0 : Math.round(part * 100.0 / whole));
    }

    private static String markdown(Map<String, Object> metrics, List<Map<String, Object>> results)
    {
        StringBuilder sb = new StringBuilder("# Videoya soru sor: değerlendirme sonucu\n\n");
        metrics.forEach((k, v) -> sb.append("- **").append(k).append(":** ").append(v).append('\n'));
        sb.append("\n| # | Seçim | Puan | Hit@1 | Hit@3 | Bulundu | Doğru | Soru | Cevap |\n|---|---|---|---|---|---|---|---|---|\n");
        for(Map<String, Object> r : results)
        {
            sb.append("| ").append(r.get("id"))
              .append(" | ").append(r.get("secim"))
              .append(" | ").append(r.get("enYuksekSkor") == null ? "-" : r.get("enYuksekSkor"))
              .append(" | ").append(r.get("hit1") == null ? "-" : ((boolean) r.get("hit1") ? "✓" : "✗"))
              .append(" | ").append(r.get("hit3") == null ? "-" : ((boolean) r.get("hit3") ? "✓" : "✗"))
              .append(" | ").append((boolean) r.get("bulundu") ? "evet" : "hayır")
              .append(" | ").append((boolean) r.get("dogru") ? "✓" : "✗")
              .append(" | ").append(cell(r.get("soru")))
              .append(" | ").append(cell(r.get("cevap")))
              .append(" |\n");
        }
        return sb.toString();
    }

    private static String cell(Object o)
    {
        return o == null ? "" : o.toString().replace("|", "\\|").replace("\n", " ");
    }

    private static String fmt(double seconds)
    {
        int s = (int) Math.round(seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
