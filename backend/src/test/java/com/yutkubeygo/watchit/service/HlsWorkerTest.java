package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.HlsResult;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//HlsWorker, sahte medya servisi ve sahte depo ile: paketin bağlanması, eski paketin silinmesi,
//bu arada dosya değiştiyse sonucun atılması, hata durumu ve dönüştürme sürerken beklemesi.
@ExtendWith(MockitoExtension.class)
class HlsWorkerTest {

    private static final String MP4 = "http://localhost:9000/watchit-images/islenmis/video.mp4";
    private static final String OLD_HLS = "http://localhost:9000/watchit-images/hls/eski/master.m3u8";
    private static final String NEW_HLS = "http://localhost:9000/watchit-images/hls/yeni/master.m3u8";

    @Mock private VideoRepository videoRepository;
    @Mock private HlsClient client;
    @Mock private FileService fileService;
    @InjectMocks private HlsWorker worker;

    private Video video;
    private Path zip;

    @BeforeEach
    void setUp() throws Exception
    {
        video = new Video();
        video.setId(1L);
        video.setVideoUrl(MP4);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setHlsStatus(VideoProcessingStatus.PENDING);
        zip = Files.createTempFile("hls-testi", ".zip");
    }

    private void pendingJob()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(videoRepository.changeHlsStatus(1L, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).thenReturn(1);
        when(client.createHls(MP4)).thenReturn(new HlsResult(zip, "720,480,360,240", 60));
        when(fileService.uploadHlsPackage(zip)).thenReturn(NEW_HLS);
    }

    @Test
    void attachesPackageDeletesOldOneAndTempZip()
    {
        video.setHlsUrl(OLD_HLS);
        pendingJob();
        when(videoRepository.finishHls(1L, MP4, NEW_HLS, VideoProcessingStatus.READY)).thenReturn(1);

        worker.process(1L);

        verify(fileService).deleteHlsPackage(OLD_HLS);
        verify(fileService, never()).deleteHlsPackage(NEW_HLS);
        assertThat(Files.exists(zip)).isFalse();
    }

    @Test
    void discardsResultWhenFileChangedMeanwhile()
    {
        pendingJob();
        when(videoRepository.finishHls(1L, MP4, NEW_HLS, VideoProcessingStatus.READY)).thenReturn(0);

        worker.process(1L);

        //Yeni paket kimseye bağlanmadı: silinir
        verify(fileService).deleteHlsPackage(NEW_HLS);
    }

    @Test
    void failureIsRecordedAndMp4KeepsPlaying()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(videoRepository.changeHlsStatus(1L, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).thenReturn(1);
        when(client.createHls(MP4)).thenThrow(new IllegalStateException("Video HLS'ye çevrilemedi."));

        worker.process(1L);

        verify(videoRepository).failHls(eq(1L), eq(MP4), eq(VideoProcessingStatus.FAILED), startsWith("HLS üretilemedi"));
        verify(videoRepository, never()).finishHls(anyLong(), any(), any(), any());
    }

    @Test
    void waitsWhileVideoIsStillBeingConverted()
    {
        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        worker.process(1L);

        verify(videoRepository, never()).changeHlsStatus(anyLong(), any(), any());
        verifyNoInteractions(client);
    }

    @Test
    void doesNothingWhenNotQueued()
    {
        video.setHlsStatus(VideoProcessingStatus.READY);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        worker.process(1L);

        verifyNoInteractions(client, fileService);
    }

    @Test
    void staleHlsIsNotOfferedToPlayer()
    {
        video.setHlsUrl(NEW_HLS);
        video.setHlsStatus(VideoProcessingStatus.READY);
        video.setHlsSourceUrl(MP4);
        assertThat(video.usableHlsUrl()).isEqualTo(NEW_HLS);

        //Sahibi dosyayı değiştirdi: eski HLS başka dosyadan üretildi, oynatıcı MP4'e döner
        video.setVideoUrl("http://localhost:9000/watchit-images/islenmis/baska.mp4");
        assertThat(video.usableHlsUrl()).isNull();

        video.setVideoUrl(MP4);
        video.setHlsStatus(VideoProcessingStatus.PROCESSING);
        assertThat(video.usableHlsUrl()).isNull();
    }
}
