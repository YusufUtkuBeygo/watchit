package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscodeResult;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//VideoProcessingWorker, sahte medya servisi ve sahte depo ile: dosya değişimi, eski dosyanın silinmesi,
//hata durumu ve transkriptin ardından başlaması.
@ExtendWith(MockitoExtension.class)
class VideoProcessingWorkerTest {

    private static final String OLD = "http://localhost:9000/watchit-images/eski.webm";
    private static final String NEW = "http://localhost:9000/watchit-images/islenmis/yeni.mp4";

    @Mock private VideoRepository videoRepository;
    @Mock private VideoProcessingClient client;
    @Mock private FileService fileService;
    @Mock private TranscriptService transcriptService;
    @Mock private HlsService hlsService;
    @InjectMocks private VideoProcessingWorker worker;

    private Video video;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception
    {
        video = new Video();
        video.setId(1L);
        video.setVideoUrl(OLD);
        video.setProcessingStatus(VideoProcessingStatus.PENDING);
        tempFile = Files.createTempFile("donusturme-testi", ".mp4");
    }

    private void pendingJob()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(videoRepository.changeProcessingStatus(1L, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).thenReturn(1);
    }

    private void transcodes() 
    {
        when(client.transcode(OLD)).thenReturn(new TranscodeResult(tempFile, "transcode", 7.6, 1280, 720));
        when(fileService.uploadProcessedVideo(tempFile)).thenReturn(NEW);
    }

    @Test
    void swapsFileDeletesOldOneAndStartsTranscript()
    {
        pendingJob();
        transcodes();
        when(videoRepository.swapProcessedFile(1L, OLD, NEW, 8, VideoProcessingStatus.READY)).thenAnswer(inv -> {
            video.setVideoUrl(NEW);
            return 1;
        });

        worker.process(1L);

        verify(fileService).deleteOwnFile(OLD);
        verify(transcriptService).start(video);
        //Çoklu kalite yeni dosyadan sıraya girer
        verify(hlsService).start(1L);
        assertThat(tempFile).doesNotExist();
    }

    @Test
    void keepsOldFileWhenAnotherVideoUsesIt()
    {
        pendingJob();
        transcodes();
        when(videoRepository.swapProcessedFile(1L, OLD, NEW, 8, VideoProcessingStatus.READY)).thenReturn(1);
        when(videoRepository.existsByVideoUrl(OLD)).thenReturn(true);

        worker.process(1L);

        verify(fileService, never()).deleteOwnFile(OLD);
    }

    @Test
    void discardsResultWhenFileChangedMeanwhile()
    {
        pendingJob();
        transcodes();
        when(videoRepository.swapProcessedFile(1L, OLD, NEW, 8, VideoProcessingStatus.READY)).thenReturn(0);

        worker.process(1L);

        //Yeni dosya silinir, eskisine dokunulmaz, transkripti yeni iş başlatacak
        verify(fileService).deleteOwnFile(NEW);
        verify(fileService, never()).deleteOwnFile(OLD);
        verifyNoInteractions(transcriptService);
        assertThat(tempFile).doesNotExist();
    }

    @Test
    void failureKeepsOriginalAndStillStartsTranscript()
    {
        pendingJob();
        when(client.transcode(OLD)).thenThrow(new IllegalStateException("Dosya video olarak okunamadı."));
        when(videoRepository.failProcessing(eq(1L), eq(OLD), eq(VideoProcessingStatus.FAILED), anyString())).thenReturn(1);

        worker.process(1L);

        verify(videoRepository).failProcessing(1L, OLD, VideoProcessingStatus.FAILED, "Video dönüştürülemedi: Dosya video olarak okunamadı.");
        verify(fileService, never()).deleteOwnFile(any());
        verify(transcriptService).start(video);
    }

    @Test
    void oldFileDeleteErrorDoesNotFailTheJob()
    {
        pendingJob();
        transcodes();
        when(videoRepository.swapProcessedFile(1L, OLD, NEW, 8, VideoProcessingStatus.READY)).thenAnswer(inv -> {
            video.setVideoUrl(NEW);
            return 1;
        });
        doThrow(new IllegalStateException("MinIO kapalı")).when(fileService).deleteOwnFile(OLD);

        worker.process(1L);

        verify(videoRepository, never()).failProcessing(any(), any(), any(), any());
        verify(transcriptService).start(video);
    }

    @Test
    void skipsWhenNotPending()
    {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        worker.process(1L);

        verifyNoInteractions(client, fileService, transcriptService);
    }

    @Test
    void skipsWhenAnotherWorkerTookTheJob()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(videoRepository.changeProcessingStatus(1L, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).thenReturn(0);

        worker.process(1L);

        verifyNoInteractions(client, fileService, transcriptService);
    }
}
