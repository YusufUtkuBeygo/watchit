package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ChapterDto;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoChapter;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.VideoChapterRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

//Otomatik bölümler: transkript hazır olunca bir kez üretilir, veritabanında saklanır, izleme sayfası okur.
//İsteğe bağlı bir süs: üretilemezse video ve soru-cevap etkilenmez (açılışta yeniden denenir).
@Service
public class ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);

    private final ChapterGenerator generator;
    private final VideoChapterRepository chapterRepository;
    private final VideoTranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final VideoRepository videoRepository;
    private final TransactionTemplate transactionTemplate;
    private final boolean runOnStartup;
    private final ChapterFrameService frameService;
    private final StorageCleanupService storageCleanupService;

    public ChapterService(ChapterGenerator generator, VideoChapterRepository chapterRepository,
                          VideoTranscriptRepository transcriptRepository, TranscriptSegmentRepository segmentRepository,
                          VideoRepository videoRepository, TransactionTemplate transactionTemplate,
                          @Value("${chapters.run-on-startup:true}") boolean runOnStartup,
                          ChapterFrameService frameService, StorageCleanupService storageCleanupService)
    {
        this.frameService = frameService;
        this.storageCleanupService = storageCleanupService;
        this.runOnStartup = runOnStartup;
        this.generator = generator;
        this.chapterRepository = chapterRepository;
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.videoRepository = videoRepository;
        this.transactionTemplate = transactionTemplate;
    }

    //GET /videos/{id}/chapters; video yoksa null (404). Bölüm yoksa boş liste
    public List<ChapterDto> getChapters(Long videoId)
    {
        if(!videoRepository.existsById(videoId))
            return null;
        return chapterRepository.findByVideoIdOrderByStartSecondsAsc(videoId).stream()
                .map(c -> new ChapterDto(c.getStartSeconds(), c.getTitle(), c.getThumbnailUrl()))
                .toList();
    }

    //Transkript işçisiyle aynı kuyrukta (birer birer): Gemini'ye aynı anda yığılmasın
    @Async("transcriptExecutor")
    public void generateAsync(Long videoId)
    {
        generate(videoId);
    }

    //Transkript işçisi kendi iş parçacığında (zaten aynı kuyruk) doğrudan çağırır
    public void generate(Long videoId)
    {
        VideoTranscript transcript = transcriptRepository.findByVideoId(videoId).orElse(null);
        if(transcript == null || transcript.getStatus() != TranscriptStatus.READY || transcript.isChaptersDone())
            return;
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null)
            return;

        List<TranscriptSegment> segments = segmentRepository.findByVideoIdOrderByStartSecondsAsc(videoId);
        List<ChapterDto> chapters;
        if(!ChapterGenerator.isEligible(video.getDurationInSeconds(), segments.size()))
        {
            chapters = List.of();    //kısa ya da az konuşmalı video: bölüm yok, yeniden denenmez
        }
        else
        {
            try
            {
                chapters = generator.generate(video.getTitle(), video.getDurationInSeconds(), segments);
            }
            catch(Exception e)
            {
                //Kota ya da yoğunluk: chaptersDone false kalır, sonraki açılışta yeniden denenir
                log.warn("Bölümler çıkarılamadı: video {}: {}", videoId, e.getMessage());
                return;
            }
        }

        //Eski bölümleri silip yenilerini yazmak ve "denendi" işareti tek transaction
        List<ChapterDto> result = chapters;
        transactionTemplate.executeWithoutResult(status -> {
            //Eski bölümlerin kareleri commit'ten sonra silinir (yeni bölümlere yeni kareler çıkarılacak)
            StorageCleanupService.Files oldFrames = new StorageCleanupService.Files();
            chapterRepository.findByVideoIdOrderByStartSecondsAsc(videoId).forEach(c -> oldFrames.addFile(c.getThumbnailUrl()));
            storageCleanupService.deleteAfterCommit(oldFrames);
            chapterRepository.deleteByVideoId(videoId);
            for(ChapterDto c : result)
                chapterRepository.save(new VideoChapter(video, c.startSeconds(), c.title()));
            transcriptRepository.markChaptersDone(videoId);
        });
        log.info("Bölümler yazıldı: video {}, {} bölüm", videoId, result.size());
        //Kareler bölümler yazıldıktan sonra: kare çıkarılamazsa bile bölümler durur
        if(!result.isEmpty())
            frameService.generate(videoId);
    }

    //Karesi eksik bölümler (özellik sonradan eklendi ya da medya servisi o an kapalıydı); aynı kuyrukta
    @Async("transcriptExecutor")
    public void generateFramesAsync(Long videoId)
    {
        frameService.generate(videoId);
    }

    //Özellik sonradan eklendi: bölümü denenmemiş hazır transkriptler açılışta sıraya girer.
    //Testlerde kapalı (build.gradle): testler gerçek yerel veritabanına bağlanıyor, Gemini'ye istek atmasınlar
    @EventListener(ApplicationReadyEvent.class)
    public void fillMissing()
    {
        if(!runOnStartup)
            return;
        for(VideoTranscript t : transcriptRepository.findByStatusAndChaptersDoneFalse(TranscriptStatus.READY))
            generateAsync(t.getVideo().getId());
        for(Long videoId : chapterRepository.findVideoIdsWithMissingFrames())
            generateFramesAsync(videoId);
    }
}
