package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ChapterDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

//Modelin cevabını okuma ve kurallara göre temizleme (Gemini çağrılmaz)
class ChapterGeneratorTest {

    private final ChapterGenerator generator = new ChapterGenerator(mock(org.springframework.ai.chat.model.ChatModel.class), "");

    @Test
    void parsesJsonEvenInsideCodeFence()
    {
        String raw = """
                İşte bölümler:
                ```json
                [{"start":"00:00","title":"Giriş"},{"start":"02:15","title":"Robot el"},{"start":"1:02:03","title":"Son"}]
                ```
                """;
        assertThat(generator.parse(raw)).containsExactly(
                new ChapterDto(0, "Giriş"), new ChapterDto(135, "Robot el"), new ChapterDto(3723, "Son"));
    }

    @Test
    void brokenAnswerGivesNoChapters()
    {
        assertThat(generator.parse("bölüm çıkaramadım")).isEmpty();
        assertThat(generator.parse("[{\"start\":\"aa\",\"title\":\"x\"}]")).isEmpty();
        assertThat(generator.parse(null)).isEmpty();
    }

    @Test
    void cleanSortsForcesZeroDropsCloseAndOutOfRange()
    {
        List<ChapterDto> raw = List.of(
                new ChapterDto(300, "Üçüncü"),
                new ChapterDto(5, "Giriş"),          //ilk bölüm 0'a çekilir
                new ChapterDto(120, "İkinci"),
                new ChapterDto(130, "Çok yakın"),    //öncekinden 10 sn sonra: atılır
                new ChapterDto(900, "Süre dışı"));   //video 600 sn: atılır
        assertThat(ChapterGenerator.clean(raw, 600)).containsExactly(
                new ChapterDto(0, "Giriş"), new ChapterDto(120, "İkinci"), new ChapterDto(300, "Üçüncü"));
    }

    @Test
    void lessThanThreeChaptersMeansNone()
    {
        assertThat(ChapterGenerator.clean(List.of(new ChapterDto(0, "A"), new ChapterDto(100, "B")), 600)).isEmpty();
    }

    @Test
    void longTitleIsShortened()
    {
        String longTitle = "x".repeat(80);
        List<ChapterDto> out = ChapterGenerator.clean(List.of(new ChapterDto(0, longTitle), new ChapterDto(60, "b"), new ChapterDto(120, "c")), 600);
        assertThat(out.getFirst().title()).hasSize(60).endsWith("…");
    }

    @Test
    void eligibility()
    {
        assertThat(ChapterGenerator.isEligible(119, 50)).isFalse();
        assertThat(ChapterGenerator.isEligible(600, 7)).isFalse();
        assertThat(ChapterGenerator.isEligible(600, 8)).isTrue();
        assertThat(ChapterGenerator.isEligible(null, 50)).isFalse();
    }
}
