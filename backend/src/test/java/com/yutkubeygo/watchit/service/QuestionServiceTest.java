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
        assertThat(system).contains("BULUNAMADI").contains("Yalnız");
        assertThat(user).contains("Video başlığı: Tears of Steel").contains("Soru: Soru?");
        assertThat(user.indexOf("[00:02 - 00:53]")).isLessThan(user.indexOf("[04:59 - 05:48]"));   //zaman sırasıyla
    }

    @Test
    void noRelevantChunkDoesNotCallGemini()
    {
        when(retriever.findRelevant(anyLong(), anyString())).thenReturn(List.of());

        QuestionResponseDto result = questionService.ask(1L, 5L, "Menemen nasıl yapılır?");

        assertThat(result.found()).isFalse();
        assertThat(result.answer()).isEqualTo(QuestionService.NOT_FOUND_ANSWER);
        verify(chatModel, never()).call(any(Prompt.class));
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
