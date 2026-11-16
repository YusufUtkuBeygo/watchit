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

    public TranscriptWorker(VideoTranscriptRepository transcriptRepository, TranscriptSegmentRepository segmentRepository,
                            TranscriptionClient transcriptionClient, TransactionTemplate transactionTemplate)
    {
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.transcriptionClient = transcriptionClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Async("transcriptExecutor")
    public void processAsync(Long videoId)
    {
        process(videoId);
    }

    //PENDING -> PROCESSING -> READY (cümleler kaydedildi) ya da FAILED (hata mesajıyla)
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

        try
        {
            TranscriptionResult result = transcriptionClient.transcribe(videoUrl);
            List<TranscriptionResult.Segment> segments = result.segments() == null ? List.of() : result.segments();

            //Eski cümleleri silmek, yenilerini yazmak ve durumu READY yapmak tek transaction: yarım iş kalmaz
            transactionTemplate.executeWithoutResult(status -> {
                segmentRepository.deleteByVideoId(videoId);
                for(TranscriptionResult.Segment s : segments)
                {
                    TranscriptSegment segment = new TranscriptSegment();
                    segment.setVideo(transcript.getVideo());
                    segment.setStartSeconds(s.start());
                    segment.setEndSeconds(s.end());
                    segment.setText(fit(s.text(), 2000));
                    segmentRepository.save(segment);
                }
                transcript.setStatus(TranscriptStatus.READY);
                transcript.setLanguage(result.language());
                transcriptRepository.save(transcript);
            });
            log.info("Transkript hazır: video {}, {} cümle, dil {}", videoId, segments.size(), result.language());
        }
        catch(Exception e)
        {
            log.warn("Transkript başarısız: video {}: {}", videoId, e.getMessage());
            transcript.setStatus(TranscriptStatus.FAILED);
            transcript.setErrorMessage(fit("Transkript çıkarılamadı: " + e.getMessage(), MAX_ERROR_LENGTH));
            transcriptRepository.save(transcript);
        }
    }

    private static String fit(String text, int max)
    {
        if(text == null)
            return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
