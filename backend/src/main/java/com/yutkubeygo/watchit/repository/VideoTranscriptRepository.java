package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.VideoTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yutkubeygo.watchit.enums.TranscriptStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoTranscriptRepository extends JpaRepository<VideoTranscript, Long> {

    Optional<VideoTranscript> findByVideoId(Long videoId);

    //Açılışta yarım kalmış işleri bulmak için
    List<VideoTranscript> findByStatusIn(Collection<TranscriptStatus> statuses);

    //Hazır ama özeti olmayanlar (özellik eklenmeden önce hazırlananlar ya da özeti yazılamayanlar)
    List<VideoTranscript> findByStatusAndSummaryIsNull(TranscriptStatus status);
}
