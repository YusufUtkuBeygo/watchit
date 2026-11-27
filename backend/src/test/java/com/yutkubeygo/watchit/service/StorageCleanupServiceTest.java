package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

//Silinen kayıtların dosyaları: kullanılmayan silinir, başka kayıtta kullanılan durur, silme commit'ten sonra olur
class StorageCleanupServiceTest {

    private static final String BASE = "http://localhost:9000/watchit-images/";

    private FileService fileService;
    private VideoRepository videoRepository;
    private ChannelRepository channelRepository;
    private StorageCleanupService service;

    @BeforeEach
    void setUp()
    {
        fileService = mock(FileService.class);
        videoRepository = mock(VideoRepository.class);
        channelRepository = mock(ChannelRepository.class);
        //Executor yerine hemen çalıştır: test arka plan işini beklemesin
        service = new StorageCleanupService(fileService, videoRepository, channelRepository, Runnable::run);
    }

    @AfterEach
    void tearDown()
    {
        if(TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    private static Video video()
    {
        Video video = new Video();
        video.setVideoUrl(BASE + "islenmis/a.mp4");
        video.setThumbnailUrl(BASE + "kapak/a.jpg");
        video.setHlsSourceUrl(BASE + "islenmis/a.mp4");
        video.setHlsUrl(BASE + "hls/123/master.m3u8");
        return video;
    }

    @Test
    void deletedVideoFilesAreRemoved()
    {
        service.deleteAfterCommit(new StorageCleanupService.Files().addVideo(video()));

        verify(fileService).deleteOwnFile(BASE + "islenmis/a.mp4");
        verify(fileService).deleteOwnFile(BASE + "kapak/a.jpg");
        verify(fileService).deleteHlsPackage(BASE + "hls/123/master.m3u8");
        //videoUrl ile hlsSourceUrl aynı dosya: bir kez silinir
        verify(fileService, times(2)).deleteOwnFile(anyString());
    }

    @Test
    void fileUsedByAnotherRecordIsKept()
    {
        //Demo verisinde aynı kapak birkaç videoda kullanılıyor
        when(videoRepository.existsByThumbnailUrl(BASE + "kapak/a.jpg")).thenReturn(true);
        when(videoRepository.existsByHlsUrl(BASE + "hls/123/master.m3u8")).thenReturn(true);

        service.deleteAfterCommit(new StorageCleanupService.Files().addVideo(video()));

        verify(fileService).deleteOwnFile(BASE + "islenmis/a.mp4");
        verify(fileService, never()).deleteOwnFile(BASE + "kapak/a.jpg");
        verify(fileService, never()).deleteHlsPackage(anyString());
    }

    @Test
    void channelPicturesAreRemoved()
    {
        Channel channel = new Channel();
        channel.setProfilePictureUrl(BASE + "profil.png");
        channel.setBannerImageUrl(BASE + "banner.png");
        when(channelRepository.existsByBannerImageUrl(BASE + "banner.png")).thenReturn(true);

        service.deleteAfterCommit(new StorageCleanupService.Files().addChannel(channel));

        verify(fileService).deleteOwnFile(BASE + "profil.png");
        verify(fileService, never()).deleteOwnFile(BASE + "banner.png");
    }

    @Test
    void oneFailureDoesNotStopTheRest()
    {
        doThrow(new IllegalStateException("MinIO kapalı")).when(fileService).deleteOwnFile(BASE + "islenmis/a.mp4");

        service.deleteAfterCommit(new StorageCleanupService.Files().addVideo(video()));

        verify(fileService).deleteOwnFile(BASE + "kapak/a.jpg");
        verify(fileService).deleteHlsPackage(BASE + "hls/123/master.m3u8");
    }

    @Test
    void insideTransactionDeletesOnlyAfterCommit()
    {
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAfterCommit(new StorageCleanupService.Files().addVideo(video()));
        //Henüz commit yok: dosyaya dokunulmadı (işlem geri alınırsa video kırık kalmasın)
        verifyNoInteractions(fileService);

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, syncs.size());
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(fileService).deleteHlsPackage(BASE + "hls/123/master.m3u8");
    }

    @Test
    void rolledBackTransactionDeletesNothing()
    {
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAfterCommit(new StorageCleanupService.Files().addVideo(video()));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(fileService);
    }

    @Test
    void replacedFileIsAddedOnlyWhenChanged()
    {
        StorageCleanupService.Files files = new StorageCleanupService.Files();
        StorageCleanupService.addReplaced(files, BASE + "eski.jpg", BASE + "yeni.jpg");
        StorageCleanupService.addReplaced(files, BASE + "ayni.jpg", BASE + "ayni.jpg");
        StorageCleanupService.addReplaced(files, null, BASE + "ilk.jpg");

        assertEquals(List.of(BASE + "eski.jpg"), List.copyOf(files.files()));
    }
}
