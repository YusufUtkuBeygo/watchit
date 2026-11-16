package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptResponseDto;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.exception.ConflictException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

//Transkriptin durumu ve başlatılması. Asıl iş (Whisper'a gitmek) TranscriptWorker'da, arka planda.
@Service
public class TranscriptService {

    //Whisper servisi de 60 dakikadan fazlasını işlemez; daha uzun videoda hiç başlatmayıp nedenini yazıyoruz
    static final int MAX_DURATION_SECONDS = 3600;

    private final VideoRepository videoRepository;
    private final VideoTranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final TranscriptWorker worker;

    public TranscriptService(VideoRepository videoRepository, VideoTranscriptRepository transcriptRepository,
                             TranscriptSegmentRepository segmentRepository, TranscriptWorker worker)
    {
        this.videoRepository = videoRepository;
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.worker = worker;
    }

    //Video yoksa null (404). Transkript hiç istenmediyse NOT_STARTED. Cümleler yalnız READY iken döner.
    public TranscriptResponseDto getTranscript(Long videoId)
    {
        if(!videoRepository.existsById(videoId))
            return null;

        VideoTranscript transcript = transcriptRepository.findByVideoId(videoId).orElse(null);
        if(transcript == null)
            return new TranscriptResponseDto(videoId, "NOT_STARTED", null, null, null, List.of());

        List<TranscriptResponseDto.Segment> segments = transcript.getStatus() != TranscriptStatus.READY ? List.of()
                : segmentRepository.findByVideoIdOrderByStartSecondsAsc(videoId).stream()
                    .map(s -> new TranscriptResponseDto.Segment(s.getStartSeconds(), s.getEndSeconds(), s.getText()))
                    .toList();

        return new TranscriptResponseDto(videoId, transcript.getStatus().name(), transcript.getLanguage(),
                transcript.getErrorMessage(), transcript.getUpdatedAt(), segments);
    }

    //POST /videos/{id}/transcript: yalnız video sahibi (403); zaten sıradaysa ya da işleniyorsa 409; video yoksa null (404)
    public TranscriptResponseDto requestTranscript(Long videoId, Long userId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null)
            return null;

        if(!video.getChannel().getOwner().getId().equals(userId))
            throw new ForbiddenException();

        VideoTranscript existing = transcriptRepository.findByVideoId(videoId).orElse(null);
        if(existing != null && (existing.getStatus() == TranscriptStatus.PENDING || existing.getStatus() == TranscriptStatus.PROCESSING))
            throw new ConflictException();

        start(video);
        return getTranscript(videoId);
    }

    //Yeni video yüklenince ya da video dosyası değişince VideoService çağırır
    public void start(Video video)
    {
        if(video.getVideoUrl() == null || video.getVideoUrl().isBlank())
            return;

        VideoTranscript transcript = transcriptRepository.findByVideoId(video.getId()).orElseGet(VideoTranscript::new);
        transcript.setVideo(video);
        transcript.setLanguage(null);

        Integer duration = video.getDurationInSeconds();
        if(duration != null && duration > MAX_DURATION_SECONDS)
        {
            transcript.setStatus(TranscriptStatus.FAILED);
            transcript.setErrorMessage("Video 60 dakikadan uzun olduğu için transkript çıkarılmadı.");
            transcriptRepository.save(transcript);
            return;
        }

        transcript.setStatus(TranscriptStatus.PENDING);
        transcript.setErrorMessage(null);
        transcriptRepository.save(transcript);
        scheduleAfterCommit(video.getId());
    }

    //Uygulama kapanırken yarım kalan işler açılışta yeniden sıraya girer
    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinished()
    {
        for(VideoTranscript t : transcriptRepository.findByStatusIn(List.of(TranscriptStatus.PENDING, TranscriptStatus.PROCESSING)))
        {
            t.setStatus(TranscriptStatus.PENDING);
            transcriptRepository.save(t);
            worker.processAsync(t.getVideo().getId());
        }
    }

    //İşçi videoyu veritabanından okuyacak: video kaydı commit edilmeden başlarsa onu göremez.
    //Bu yüzden içinde bulunduğumuz transaction bitince (afterCommit) sıraya koyuyoruz.
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
