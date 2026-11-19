package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

//Video dönüştürmeyi başlatır. Asıl iş VideoProcessingWorker'da, arka planda; transkript dönüştürmeden sonra başlar.
@Service
public class VideoProcessingService {

    private final VideoRepository videoRepository;
    private final VideoProcessingWorker worker;

    public VideoProcessingService(VideoRepository videoRepository, VideoProcessingWorker worker)
    {
        this.videoRepository = videoRepository;
        this.worker = worker;
    }

    //Yeni video yüklenince ya da video dosyası değişince VideoService çağırır
    public void start(Video video)
    {
        if(video.getVideoUrl() == null || video.getVideoUrl().isBlank())
            return;

        video.setProcessingStatus(VideoProcessingStatus.PENDING);
        video.setProcessingError(null);
        videoRepository.save(video);
        scheduleAfterCommit(video.getId());
    }

    //Dönüştürme sürüyor mu (sürerken transkript elle başlatılamaz: dosya birazdan değişecek)
    public static boolean isRunning(Video video)
    {
        return video.getProcessingStatus() == VideoProcessingStatus.PENDING
                || video.getProcessingStatus() == VideoProcessingStatus.PROCESSING;
    }

    //Uygulama kapanırken yarım kalan işler açılışta yeniden sıraya girer
    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinished()
    {
        for(Video v : videoRepository.findByProcessingStatusIn(List.of(VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)))
        {
            v.setProcessingStatus(VideoProcessingStatus.PENDING);
            videoRepository.save(v);
            worker.processAsync(v.getId());
        }
    }

    //İşçi videoyu veritabanından okuyacak: kayıt commit edilmeden başlarsa onu göremez
    private void scheduleAfterCommit(Long videoId)
    {
        if(TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit()
                {
                    worker.processAsync(videoId);
                }
            });
        }
        else
        {
            worker.processAsync(videoId);
        }
    }
}
