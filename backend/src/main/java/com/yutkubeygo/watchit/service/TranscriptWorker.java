package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptionResult;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

//Arka plan işçisi: transkripti çıkarır ve kaydeder. Ayrı sınıf olmasının sebebi: @Async yalnız
//başka bir sınıftan çağrılınca çalışır (Spring araya bir vekil koyar, aynı sınıfın içinden çağrı vekilden geçmez).
@Component
public class TranscriptWorker {

    private static final Logger log = LoggerFactory.getLogger(TranscriptWorker.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final VideoTranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final TranscriptionClient transcriptionClient;
    private final TransactionTemplate transactionTemplate;
    private final TranscriptIndexer indexer;
    private final TranscriptSummarizer summarizer;

    public TranscriptWorker(VideoTranscriptRepository transcriptRepository, TranscriptSegmentRepository segmentRepository,
                            TranscriptionClient transcriptionClient, TransactionTemplate transactionTemplate,
                            TranscriptIndexer indexer, TranscriptSummarizer summarizer)
    {
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.transcriptionClient = transcriptionClient;
        this.transactionTemplate = transactionTemplate;
        this.indexer = indexer;
        this.summarizer = summarizer;
    }

    @Async("transcriptExecutor")
    public void processAsync(Long videoId)
    {
        process(videoId);
    }

    //PENDING -> PROCESSING -> READY (cümleler kaydedildi ve parçalar vektöre çevrildi: soru sorulabilir)
    //ya da FAILED (hata mesajıyla)
    public void process(Long videoId)
    {
        VideoTranscript transcript = transcriptRepository.findByVideoId(videoId).orElse(null);
        //Video bu arada silindiyse (silme zinciri transkripti de siler) ya da iş iptal olduysa yapılacak bir şey yok
        if(transcript == null || transcript.getStatus() != TranscriptStatus.PENDING)
            return;

        String videoUrl = transcript.getVideo().getVideoUrl();
        transcript.setStatus(TranscriptStatus.PROCESSING);
        transcript.setErrorMessage(null);
        transcriptRepository.save(transcript);
        log.info("Transkript başladı: video {}", videoId);

        List<TranscriptSegment> saved = new ArrayList<>();
        try
        {
            TranscriptionResult result = transcriptionClient.transcribe(videoUrl);
            List<TranscriptionResult.Segment> segments = result.segments() == null ? List.of() : result.segments();

            //Eski cümleleri silmek ve yenilerini yazmak tek transaction: yarım iş kalmaz
            transactionTemplate.executeWithoutResult(status -> {
                segmentRepository.deleteByVideoId(videoId);
                for(TranscriptionResult.Segment s : segments)
                {
                    TranscriptSegment segment = new TranscriptSegment();
                    segment.setVideo(transcript.getVideo());
                    segment.setStartSeconds(s.start());
                    segment.setEndSeconds(s.end());
                    segment.setText(fit(s.text(), 2000));
                    saved.add(segmentRepository.save(segment));
                }
                transcript.setLanguage(result.language());
                transcriptRepository.save(transcript);
            });
            log.info("Transkript çıkarıldı: video {}, {} cümle, dil {}", videoId, segments.size(), result.language());
        }
        catch(Exception e)
        {
            fail(transcript, "Transkript çıkarılamadı: " + e.getMessage());
            return;
        }

        //Parçalara böl, Gemini ile vektöre çevir, vector_store'a yaz
        try
        {
            int chunks = indexer.index(videoId, saved);
            transcript.setStatus(TranscriptStatus.READY);
            transcriptRepository.save(transcript);
            log.info("Transkript hazır: video {}, {} parça vektöre çevrildi", videoId, chunks);
        }
        catch(Exception e)
        {
            fail(transcript, "Transkript çıkarıldı ama vektöre çevrilemedi (Gemini): " + e.getMessage());
            return;
        }

        writeSummary(transcript, saved);
    }

    //Özellik sonradan eklendi: özeti olmayan hazır transkriptler için açılışta çağrılır
    @Async("transcriptExecutor")
    public void summarizeAsync(Long videoId)
    {
        VideoTranscript transcript = transcriptRepository.findByVideoId(videoId).orElse(null);
        if(transcript == null || transcript.getStatus() != TranscriptStatus.READY || transcript.getSummary() != null)
            return;
        writeSummary(transcript, segmentRepository.findByVideoIdOrderByStartSecondsAsc(videoId));
    }

    //Özet isteğe bağlı: yazılamazsa transkript yine READY kalır, özet soruları normal aramaya düşer
    private void writeSummary(VideoTranscript transcript, List<TranscriptSegment> segments)
    {
        try
        {
            transcript.setSummary(summarizer.summarize(transcript.getVideo().getTitle(), segments));
            transcriptRepository.save(transcript);
            log.info("Özet yazıldı: video {}", transcript.getVideo().getId());
        }
        catch(Exception e)
        {
            log.warn("Özet yazılamadı: video {}: {}", transcript.getVideo().getId(), e.getMessage());
        }
    }

    private void fail(VideoTranscript transcript, String message)
    {
        log.warn("Transkript başarısız: video {}: {}", transcript.getVideo().getId(), message);
        transcript.setStatus(TranscriptStatus.FAILED);
        transcript.setErrorMessage(fit(message, MAX_ERROR_LENGTH));
        transcriptRepository.save(transcript);
    }

    private static String fit(String text, int max)
    {
        if(text == null)
            return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
