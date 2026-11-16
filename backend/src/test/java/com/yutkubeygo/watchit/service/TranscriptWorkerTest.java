package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptionResult;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TranscriptWorker.process: Whisper başarılıysa cümleler kaydedilip READY olur, hata verirse FAILED + mesaj.
//Whisper yerine sahte istemci kullanılır.
@ExtendWith(MockitoExtension.class)
class TranscriptWorkerTest {

    @Mock private VideoTranscriptRepository transcriptRepository;
    @Mock private TranscriptSegmentRepository segmentRepository;
    @Mock private TranscriptionClient transcriptionClient;

    private TranscriptWorker worker;
    private VideoTranscript transcript;

    @BeforeEach
    void setUp()
    {
        //Gerçek TransactionTemplate, sahte transaction yöneticisiyle: içindeki kod normal çalışır
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        worker = new TranscriptWorker(transcriptRepository, segmentRepository, transcriptionClient, transactionTemplate);

        Video video = new Video();
        video.setId(7L);
        video.setVideoUrl("http://localhost:9000/watchit-images/video.mp4");

        transcript = new VideoTranscript();
        transcript.setVideo(video);
        transcript.setStatus(TranscriptStatus.PENDING);
    }

    @Test
    void successSavesSegmentsAndMarksReady()
    {
        when(transcriptRepository.findByVideoId(7L)).thenReturn(Optional.of(transcript));
        when(transcriptionClient.transcribe("http://localhost:9000/watchit-images/video.mp4")).thenReturn(
                new TranscriptionResult("en", List.of(
                        new TranscriptionResult.Segment(0.0, 4.5, "Hello."),
                        new TranscriptionResult.Segment(4.5, 9.0, "Welcome to the video."))));

        worker.process(7L);

        assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.READY);
        assertThat(transcript.getLanguage()).isEqualTo("en");
        verify(segmentRepository).deleteByVideoId(7L);
        ArgumentCaptor<TranscriptSegment> captor = ArgumentCaptor.forClass(TranscriptSegment.class);
        verify(segmentRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(TranscriptSegment::getText).containsExactly("Hello.", "Welcome to the video.");
    }

    @Test
    void failureMarksFailedWithMessage()
    {
        when(transcriptRepository.findByVideoId(7L)).thenReturn(Optional.of(transcript));
        when(transcriptionClient.transcribe(anyString())).thenThrow(new RuntimeException("Whisper servisine bağlanılamadı"));

        worker.process(7L);

        assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.FAILED);
        assertThat(transcript.getErrorMessage()).contains("Whisper servisine bağlanılamadı");
        verify(segmentRepository, never()).save(any());
    }

    @Test
    void notPendingIsSkipped()
    {
        transcript.setStatus(TranscriptStatus.READY);
        when(transcriptRepository.findByVideoId(7L)).thenReturn(Optional.of(transcript));

        worker.process(7L);

        verify(transcriptionClient, never()).transcribe(anyString());
    }

    @Test
    void deletedVideoIsSkipped()
    {
        when(transcriptRepository.findByVideoId(7L)).thenReturn(Optional.empty());

        worker.process(7L);

        verify(transcriptionClient, never()).transcribe(anyString());
    }
}
