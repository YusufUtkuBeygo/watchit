package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//Yapay zekâ altyapısı (V11) gerçek PostgreSQL'e karşı:
//tablolar, video silinince transkriptin de silinmesi, pgvector'ın "en yakını bul" sorgusu, Spring AI'ın tabloya bağlanması.
//Gemini'ye istek atılmaz (anahtar gerekmez). @Transactional: her test sonunda eklenenler geri alınır.
@SpringBootTest
@Transactional
class AiInfrastructureTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoTranscriptRepository transcriptRepository;
    @Autowired private TranscriptSegmentRepository segmentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private VectorStore vectorStore;

    private Video video;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUsername("yz_testi_kullanici");
        user.setEmail("yz_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);

        Channel channel = new Channel();
        channel.setChannelName("YZ testi kanalı");
        channel.setHandle("yz_testi_kanali");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Menemen nasıl yapılır");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(600);
        videoRepository.saveAndFlush(video);
    }

    private TranscriptSegment segment(double start, double end, String text)
    {
        TranscriptSegment segment = new TranscriptSegment();
        segment.setVideo(video);
        segment.setStartSeconds(start);
        segment.setEndSeconds(end);
        segment.setText(text);
        return segment;
    }

    //768 kutulu bir vektör: verilen kutular 1, gerisi 0 (pgvector'ın metin biçiminde: "[1,0,0,...]")
    private String vector(int... hotIndexes)
    {
        float[] values = new float[768];
        for (int i : hotIndexes)
            values[i] = 1f;
        return Arrays.toString(values).replace(" ", "");
    }

    @Test
    void transcriptAndSegmentsAreSavedInOrder()
    {
        VideoTranscript transcript = new VideoTranscript();
        transcript.setVideo(video);
        transcript.setStatus(TranscriptStatus.READY);
        transcript.setLanguage("tr");
        transcriptRepository.saveAndFlush(transcript);

        segmentRepository.saveAndFlush(segment(12, 18, "Soğanı yağda kavuruyoruz."));
        segmentRepository.saveAndFlush(segment(5, 12, "Merhaba, bugün menemen yapacağız."));

        assertThat(transcriptRepository.findByVideoId(video.getId())).get()
                .extracting(VideoTranscript::getStatus).isEqualTo(TranscriptStatus.READY);
        assertThat(segmentRepository.findByVideoIdOrderByStartSecondsAsc(video.getId()))
                .extracting(TranscriptSegment::getText)
                .containsExactly("Merhaba, bugün menemen yapacağız.", "Soğanı yağda kavuruyoruz.");
    }

    @Test
    void deletingVideoDeletesTranscriptAndSegments()
    {
        VideoTranscript transcript = new VideoTranscript();
        transcript.setVideo(video);
        transcript.setStatus(TranscriptStatus.PENDING);
        transcriptRepository.saveAndFlush(transcript);
        segmentRepository.saveAndFlush(segment(0, 5, "Merhaba."));

        //Veritabanının kendi silme zinciri (ON DELETE CASCADE) çalışsın diye SQL ile silinir
        entityManager.clear();
        jdbcTemplate.update("DELETE FROM videos WHERE id = ?", video.getId());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM video_transcripts WHERE video_id = ?", Long.class, video.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM transcript_segments WHERE video_id = ?", Long.class, video.getId())).isZero();
    }

    @Test
    void pgvectorFindsNearestVector()
    {
        String insert = "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)";
        String metadata = "{\"videoId\": " + video.getId() + "}";
        jdbcTemplate.update(insert, "yemek parçası", metadata, vector(0, 1));
        jdbcTemplate.update(insert, "temizlik parçası", metadata, vector(2, 3));
        jdbcTemplate.update(insert, "araba parçası", metadata, vector(4, 5));

        //<=> : kosinüs uzaklığı (küçük = daha benzer). Soru vektörü "yemek" kutularına yakın.
        List<String> nearest = jdbcTemplate.queryForList(
                "SELECT content FROM vector_store WHERE video_id = ? ORDER BY embedding <=> ?::vector LIMIT 2",
                String.class, video.getId(), vector(0, 1, 2));

        assertThat(nearest).containsExactly("yemek parçası", "temizlik parçası");
    }

    @Test
    void deletingVideoDeletesItsVectors()
    {
        //V12: video_id, metadata'daki videoId'den hesaplanır ve videoya ON DELETE CASCADE ile bağlıdır
        jdbcTemplate.update("INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
                "parça", "{\"videoId\": " + video.getId() + ", \"start\": 0.0}", vector(0));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store WHERE video_id = ?", Long.class, video.getId())).isOne();

        entityManager.clear();
        jdbcTemplate.update("DELETE FROM videos WHERE id = ?", video.getId());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store WHERE video_id = ?", Long.class, video.getId())).isZero();
    }

    @Test
    void springAiVectorStoreIsConfigured()
    {
        //Spring AI'ın PgVectorStore'u V11'deki tabloya bağlı olarak açıldı mı (Gemini'ye istek atmaz)
        assertThat(vectorStore.getName()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject("SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class))
                .isEqualTo("0.8.7");
    }
}
