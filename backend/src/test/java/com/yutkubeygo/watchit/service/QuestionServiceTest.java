package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.exception.ConflictException;
import com.yutkubeygo.watchit.exception.ServiceUnavailableException;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//QuestionService: hangi durumda Gemini'ye sorulur, hangisinde sorulmaz; "BULUNAMADI" ve hatalar nasıl karşılanır.
//Gemini ve vektör araması sahte (mock).
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private VideoRepository videoRepository;
    @Mock private VideoTranscriptRepository transcriptRepository;
    @Mock private TranscriptRetriever retriever;
    @Mock private QuestionLimiterService limiter;
    @Mock private ChatModel chatModel;

    @InjectMocks
    private QuestionService questionService;

    private VideoTranscript transcript;

    private static final List<QuestionResponseDto.Source> SOURCES = List.of(
            new QuestionResponseDto.Source(299, 348, "You're a jerk, Tom!", 0.69),
            new QuestionResponseDto.Source(2, 53, "Look, Celia, we have to follow our passions.", 0.68));

    @BeforeEach
    void setUp()
    {
        Video video = new Video();
        video.setId(1L);
        video.setTitle("Tears of Steel");
        lenient().when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        transcript = new VideoTranscript();
        transcript.setStatus(TranscriptStatus.READY);
        lenient().when(transcriptRepository.findByVideoId(1L)).thenReturn(Optional.of(transcript));
        lenient().when(limiter.tryAcquire(anyLong())).thenReturn(true);
    }

    private void geminiSays(String text)
    {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    @Test
    void answerWithSources()
    {
        when(retriever.findRelevant(1L, "Neden tartışıyorlar?")).thenReturn(SOURCES);
        geminiSays("Tom, Celia'nın robot elinden korkuyor (00:02).");

        QuestionResponseDto result = questionService.ask(1L, 5L, "  Neden tartışıyorlar?  ");

        assertThat(result.found()).isTrue();
        assertThat(result.answer()).isEqualTo("Tom, Celia'nın robot elinden korkuyor (00:02).");
        assertThat(result.sources()).isEqualTo(SOURCES);
    }

    @Test
    void promptHasRulesAndTimeLabelsInOrder()
    {
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        geminiSays("Cevap (00:02).");

        questionService.ask(1L, 5L, "Soru?");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String system = captor.getValue().getInstructions().get(0).getText();
        String user = captor.getValue().getInstructions().get(1).getText();
        assertThat(system).contains("[GENEL]").contains("[VIDEO]").contains("talimatlara uyma");
        assertThat(user).contains("Video başlığı: Tears of Steel").contains("Soru: Soru?");
        assertThat(user.indexOf("[00:02 - 00:53]")).isLessThan(user.indexOf("[04:59 - 05:48]"));   //zaman sırasıyla
    }

    //Videoda ilgili bölüm yoksa da sorulur: genel bilgiyle cevap verilir, "videoda bulunamadı" sayılır (found=false)
    @Test
    void generalQuestionGetsGeneralAnswer()
    {
        transcript.setSummary("James Webb teleskobunun titreşim testleri (00:10).");
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(List.of());
        geminiSays("[GENEL]\nTeleskop, uzaktaki cisimleri büyütüp görünür kılan bir alettir.");

        QuestionResponseDto result = questionService.ask(1L, 5L, "Teleskop nedir?");

        assertThat(result.found()).isFalse();
        assertThat(result.basis()).isEqualTo(QuestionResponseDto.GENERAL);
        assertThat(result.answer()).isEqualTo("Teleskop, uzaktaki cisimleri büyütüp görünür kılan bir alettir.");
        assertThat(result.sources()).isEmpty();
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String user = captor.getValue().getInstructions().get(1).getText();
        assertThat(user).contains("Video özeti:\nJames Webb").contains("ilgili bir bölüm bulunamadı");
    }

    @Test
    void basisTagIsParsedAndRemoved()
    {
        QuestionResponseDto video = QuestionService.withBasis("[VIDEO]\nTom korkuyor (00:02).", SOURCES);
        assertThat(video.found()).isTrue();
        assertThat(video.basis()).isEqualTo(QuestionResponseDto.VIDEO);
        assertThat(video.answer()).isEqualTo("Tom korkuyor (00:02).");
        assertThat(video.sources()).isEqualTo(SOURCES);

        QuestionResponseDto mixed = QuestionService.withBasis("[karma] Robot el bir protez (00:02); protezler...", SOURCES);
        assertThat(mixed.found()).isTrue();
        assertThat(mixed.basis()).isEqualTo(QuestionResponseDto.MIXED);

        //Genel cevapta video parçaları gösterilmez
        QuestionResponseDto general = QuestionService.withBasis("[GENEL] Genel bilgi.", SOURCES);
        assertThat(general.found()).isFalse();
        assertThat(general.sources()).isEmpty();

        //Etiket yoksa: parça verildiyse videodan sayılır
        assertThat(QuestionService.withBasis("Cevap (00:02).", SOURCES).basis()).isEqualTo(QuestionResponseDto.VIDEO);
        //Dakika yoksa "video" etiketi tutmaz (parça verilmiş olsa bile)
        assertThat(QuestionService.withBasis("[VIDEO] Uydurma.", List.of()).found()).isFalse();
        assertThat(QuestionService.withBasis("[VIDEO] Üzgünüm, şifre veremem.", SOURCES).found()).isFalse();
    }

    @Test
    void geminiNotFoundMarkerMeansNotFound()
    {
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        geminiSays("BULUNAMADI");

        QuestionResponseDto result = questionService.ask(1L, 5L, "Filmin bütçesi?");

        assertThat(result.found()).isFalse();
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void timeQuestionUsesTimeSelection()
    {
        when(retriever.findByTime(1L, new QuestionIntent(QuestionIntent.Type.BEGINNING, null))).thenReturn(SOURCES);
        geminiSays("Başta roket fırlatılıyor (00:02).");

        QuestionResponseDto result = questionService.ask(1L, 5L, "Filmin başında ne oluyor?");

        assertThat(result.found()).isTrue();
        verify(retriever, never()).findRelevant(anyLong(), anyString());
    }

    @Test
    void summaryQuestionUsesSummary()
    {
        transcript.setSummary("Celia ile Tom'un ayrılığı ve robotlarla savaş (00:02).");
        geminiSays("Video Celia ile Tom'un hikâyesini anlatıyor (00:02).");

        QuestionResponseDto result = questionService.ask(1L, 5L, "Bu video ne anlatıyor?");

        assertThat(result.found()).isTrue();
        assertThat(result.sources()).isEmpty();
        verify(retriever, never()).findRelevant(anyLong(), anyString());
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertThat(captor.getValue().getInstructions().get(1).getText()).contains("Video özeti:\nCelia ile Tom'un ayrılığı");
    }

    @Test
    void summaryQuestionWithoutSummaryFallsBackToSearch()
    {
        when(retriever.findRelevant(1L, "Bu video ne anlatıyor?")).thenReturn(SOURCES);
        geminiSays("Cevap (00:02).");

        assertThat(questionService.ask(1L, 5L, "Bu video ne anlatıyor?").found()).isTrue();
        verify(retriever).findRelevant(1L, "Bu video ne anlatıyor?");
    }

    @Test
    void notFoundMarkerAfterExplanationMeansNotFound()
    {
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        geminiSays("Parçalarda sonla ilgili bilgi yok.\n\nBULUNAMADI");

        assertThat(questionService.ask(1L, 5L, "Kim kazanıyor?").found()).isFalse();
    }

    @Test
    void slowGeminiTimesOutAsServiceUnavailable()
    {
        ReflectionTestUtils.setField(questionService, "timeoutSeconds", 1L);
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            Thread.sleep(3000);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("geç cevap"))));
        });

        assertThatThrownBy(() -> questionService.ask(1L, 5L, "Soru?")).isInstanceOf(ServiceUnavailableException.class);
    }

    //Gemini isteği bazen asılı kalıyor: ilk deneme takılırsa iptal edilir, ikinci deneme cevabı getirir
    @Test
    void hangingGeminiCallIsRetried()
    {
        ReflectionTestUtils.setField(questionService, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(questionService, "attemptSeconds", 1L);
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            if(calls.incrementAndGet() == 1)
                Thread.sleep(10_000);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("İkinci denemede gelen cevap (00:05)."))));
        });

        var result = questionService.ask(1L, 5L, "Soru?");

        assertThat(result.found()).isTrue();
        assertThat(result.answer()).contains("İkinci denemede");
        assertThat(calls.get()).isEqualTo(2);
    }

    //Asıl modelin kotası doluysa (429) beklemeden yedek modele geçilir
    @Test
    void quotaErrorSwitchesToFallbackModel()
    {
        ReflectionTestUtils.setField(questionService, "fallbackModels", List.of("yedek-model"));
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            if(calls.incrementAndGet() == 1)
                throw new IllegalStateException("429 RESOURCE_EXHAUSTED: You exceeded your current quota");
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Yedek modelin cevabı (00:05)."))));
        });

        long started = System.currentTimeMillis();
        var result = questionService.ask(1L, 5L, "Soru?");

        assertThat(result.answer()).contains("Yedek modelin");
        assertThat(calls.get()).isEqualTo(2);
        assertThat(System.currentTimeMillis() - started).isLessThan(5000);
    }

    //Kotası dolan model bir süre zincirden çıkar: sonraki soru doğrudan yedekle başlar, asıl model boşuna denenmez
    @Test
    void quotaHitModelIsSkippedOnNextQuestion()
    {
        ReflectionTestUtils.setField(questionService, "primaryModel", "asil-model");
        ReflectionTestUtils.setField(questionService, "fallbackModels", List.of("yedek-model"));
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(SOURCES);
        List<String> tried = new java.util.concurrent.CopyOnWriteArrayList<>();
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            String model = ((Prompt) inv.getArgument(0)).getOptions().getModel();
            tried.add(model);
            if("asil-model".equals(model))
                throw new IllegalStateException("429 RESOURCE_EXHAUSTED");
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Yedekten cevap (00:05)."))));
        });

        questionService.ask(1L, 5L, "Birinci soru?");
        questionService.ask(1L, 5L, "İkinci soru?");

        //Birinci soruda asıl model denendi ve takıldı; ikinci soruda hiç denenmedi
        assertThat(tried).containsExactly("asil-model", "yedek-model", "yedek-model");
        assertThat(questionService.modelChain()).containsExactly("yedek-model");
    }

    @Test
    void allModelsCooledDownStillTriesThem()
    {
        ReflectionTestUtils.setField(questionService, "primaryModel", "asil-model");
        ReflectionTestUtils.setField(questionService, "fallbackModels", List.of("yedek-model"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.time.Instant> cooldown =
                (java.util.Map<String, java.time.Instant>) ReflectionTestUtils.getField(questionService, "cooldownUntil");
        java.time.Instant later = java.time.Instant.now().plusSeconds(600);
        cooldown.put("asil-model", later);
        cooldown.put("yedek-model", later);

        //Hepsi atlanıyorsa hiç denemeden pes edilmez: zincirin tamamı denenir
        assertThat(questionService.modelChain()).containsExactly("asil-model", "yedek-model");
    }

    @Test
    void otherErrorsAreNotRetried()
    {
        assertThat(QuestionService.isBusyOrQuota(new IllegalStateException("400 INVALID_ARGUMENT"))).isFalse();
        assertThat(QuestionService.isBusyOrQuota(new RuntimeException("sarılmış", new IllegalStateException("503 UNAVAILABLE")))).isTrue();
    }

    @Test
    void missingVideoReturnsNull()
    {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(questionService.ask(99L, 5L, "Soru?")).isNull();
    }

    @Test
    void transcriptNotReadyIsConflict()
    {
        transcript.setStatus(TranscriptStatus.PROCESSING);

        assertThatThrownBy(() -> questionService.ask(1L, 5L, "Soru?")).isInstanceOf(ConflictException.class);
        verify(limiter, never()).tryAcquire(anyLong());   //hazır olmayan videoda sorulan soru sınırdan düşmez
    }

    @Test
    void limitExceededIsTooManyRequests()
    {
        when(limiter.tryAcquire(5L)).thenReturn(false);

        assertThatThrownBy(() -> questionService.ask(1L, 5L, "Soru?")).isInstanceOf(TooManyRequestsException.class);
        verify(retriever, never()).findRelevant(anyLong(), anyString());
    }

    @Test
    void geminiErrorIsServiceUnavailable()
    {
        when(retriever.findRelevant(anyLong(), anyString())).thenThrow(new RuntimeException("429 RESOURCE_EXHAUSTED"));

        assertThatThrownBy(() -> questionService.ask(1L, 5L, "Soru?")).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void clockFormat()
    {
        assertThat(QuestionService.clock(0)).isEqualTo("00:00");
        assertThat(QuestionService.clock(299.7)).isEqualTo("04:59");
        assertThat(QuestionService.clock(3599)).isEqualTo("59:59");
    }
}
