package com.yutkubeygo.watchit.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    //Videonun cümleleri, zaman sırasıyla
    List<TranscriptSegment> findByVideoIdOrderByStartSecondsAsc(Long videoId);

    //Transkript yeniden çıkarılınca eski cümleler silinir
    void deleteByVideoId(Long videoId);

    //Transkriptte kelime araması (anlam değil, kelimenin kendisi). word: özel karakterleri kaçırılmış metin.
    //Düzenli ifadede \\m ve \\M kelime başı/sonu ("tel" -> "telescope" içinde eşleşmez). Harf/aksan farkı gözetmez:
    //metne de kelimeye de lower + unaccent uygulanır (kelime sınırı işaretleri bunun dışında: lower('\\M') '\\m' olurdu)
    //Sonuç: [video id, kaç cümlede geçiyor], en çok geçen önce
    @Query(value = """
            SELECT ts.video_id, count(*) AS hits FROM transcript_segments ts
            WHERE lower(unaccent(ts.text)) ~ ('\\m' || lower(unaccent(:pattern)) || '\\M')
            GROUP BY ts.video_id ORDER BY hits DESC, ts.video_id DESC
            """,
            countQuery = "SELECT count(DISTINCT ts.video_id) FROM transcript_segments ts WHERE lower(unaccent(ts.text)) ~ ('\\m' || lower(unaccent(:pattern)) || '\\M')",
            nativeQuery = true)
    Page<Object[]> searchVideos(@Param("pattern") String pattern, Pageable pageable);

    //O videoda kelimenin geçtiği ilk birkaç cümle (zaman sırasıyla)
    @Query(value = """
            SELECT * FROM transcript_segments ts
            WHERE ts.video_id = :videoId AND lower(unaccent(ts.text)) ~ ('\\m' || lower(unaccent(:pattern)) || '\\M')
            ORDER BY ts.start_seconds LIMIT :max
            """, nativeQuery = true)
    List<TranscriptSegment> findMatches(@Param("videoId") Long videoId, @Param("pattern") String pattern, @Param("max") int max);
}
