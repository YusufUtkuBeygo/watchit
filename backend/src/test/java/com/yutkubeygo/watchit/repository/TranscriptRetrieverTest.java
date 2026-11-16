package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.service.TranscriptRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

//TranscriptRetriever gerçek PostgreSQL'e (pgvector) karşı: en yakın parçalar sırayla gelir, eşiğin altı atılır,
//başka videonun parçaları karışmaz. Gemini yerine sahte EmbeddingModel: soru vektörünü test belirler.
@SpringBootTest
@Transactional
class TranscriptRetrieverTest {

    @Autowired private TranscriptRetriever retriever;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private Video video;
    private Video otherVideo;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUsername("arayici_testi");
        user.setEmail("arayici_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);

        Channel channel = new Channel();
        channel.setChannelName("Arayıcı testi");
        channel.setHandle("arayici_testi");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);

        video = saveVideo(channel, "Menemen");
        otherVideo = saveVideo(channel, "Başka video");

        insert(video, 0, 45, "yemek parçası", vector(0, 1));
        insert(video, 35, 80, "temizlik parçası", vector(2, 3));
        insert(video, 70, 115, "araba parçası", vector(4, 5));
        insert(otherVideo, 0, 45, "başka videonun yemek parçası", vector(0, 1));
    }

    private Video saveVideo(Channel channel, String title)
    {
        Video v = new Video();
        v.setTitle(title);
        v.setChannel(channel);
        v.setCategory(categoryRepository.findAll().getFirst());
        v.setDurationInSeconds(120);
        return videoRepository.saveAndFlush(v);
    }

    private void insert(Video v, double start, double end, String text, float[] embedding)
    {
        jdbcTemplate.update("INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
                text, "{\"videoId\": " + v.getId() + ", \"start\": " + start + ", \"end\": " + end + "}",
                Arrays.toString(embedding).replace(" ", ""));
    }

    //768 kutulu vektör: verilen kutular 1, gerisi 0
    private static float[] vector(int... hot)
    {
        float[] values = new float[768];
        for(int i : hot)
            values[i] = 1f;
        return values;
    }

    private void questionVector(float[] v)
    {
        when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(new EmbeddingResponse(List.of(new Embedding(v, 0))));
    }

    @Test
    void nearestFirstAndBelowThresholdDropped()
    {
        //"yemek" kutularına en yakın (0.77), "temizlik"e eşiğin biraz üstünde (0.62), "araba"ya hiç (0)
        float[] question = vector(0, 1, 2);
        question[3] = 0.6f;
        questionVector(question);

        List<QuestionResponseDto.Source> found = retriever.findRelevant(video.getId(), "Soğan konuyor mu?");

        assertThat(found).extracting(QuestionResponseDto.Source::text).containsExactly("yemek parçası", "temizlik parçası");
        assertThat(found.getFirst().score()).isGreaterThan(found.get(1).score());
        assertThat(found.getFirst().start()).isEqualTo(0);
        assertThat(found.getFirst().end()).isEqualTo(45);
    }

    @Test
    void unrelatedQuestionFindsNothing()
    {
        questionVector(vector(100, 101));

        assertThat(retriever.findRelevant(video.getId(), "Borsa bugün ne oldu?")).isEmpty();
    }

    @Test
    void otherVideosChunksAreNotMixed()
    {
        questionVector(vector(0, 1));

        assertThat(retriever.findRelevant(video.getId(), "Soğan?"))
                .extracting(QuestionResponseDto.Source::text)
                .doesNotContain("başka videonun yemek parçası");
    }
}
