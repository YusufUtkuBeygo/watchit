package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

//HLS'yi (çoklu kalite) başlatır. Asıl iş HlsWorker'da, arka planda.
//Ne zaman: video dönüştürmesi bitince (başarılı ya da değil) ve açılışta (yarım kalanlar + hiç HLS'si olmayan eski videolar).
@Service
public class HlsService {

    private static final Logger log = LoggerFactory.getLogger(HlsService.class);

    private final VideoRepository videoRepository;
    private final HlsWorker worker;
    //Açılış işleri: uygulamada açık; Gradle testlerinde kapalı (testler gerçek yerel veritabanına bağlanıyor,
    //her test açılışında bütün eski videolar sıraya girmesin)
    private final boolean runOnStartup;

    public HlsService(VideoRepository videoRepository, HlsWorker worker,
                      @Value("${hls.run-on-startup:true}") boolean runOnStartup)
    {
        this.videoRepository = videoRepository;
        this.worker = worker;
        this.runOnStartup = runOnStartup;
    }

    //Videoyu HLS sırasına koyar. Kayıt commit edilmeden işçi başlarsa onu göremez: commit'ten sonra başlar
    public void start(Long videoId)
    {
        videoRepository.queueHls(videoId, VideoProcessingStatus.PENDING);
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

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup()
    {
        if(!runOnStartup)
            return;

        //Uygulama kapanırken yarım kalanlar baştan
        List<Video> unfinished = videoRepository.findByHlsStatusIn(
                List.of(VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING));
        unfinished.forEach(v -> start(v.getId()));

        //HLS'si hiç olmayan (bu özellikten önce yüklenmiş) ya da eskimiş videolar; tek tek, sırayla işlenir
        List<Video> backfill = videoRepository.findHlsBackfillCandidates(
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.FAILED), VideoProcessingStatus.READY);
        backfill.forEach(v -> start(v.getId()));

        if(!unfinished.isEmpty() || !backfill.isEmpty())
            log.info("HLS sırası: {} yarım kalan, {} eski video", unfinished.size(), backfill.size());
    }
}
