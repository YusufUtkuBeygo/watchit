package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    //Videonun cümleleri, zaman sırasıyla
    List<TranscriptSegment> findByVideoIdOrderByStartSecondsAsc(Long videoId);

    //Transkript yeniden çıkarılınca eski cümleler silinir
    void deleteByVideoId(Long videoId);
}
