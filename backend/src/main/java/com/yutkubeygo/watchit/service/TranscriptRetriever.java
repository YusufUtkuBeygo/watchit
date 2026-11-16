package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.QuestionResponseDto;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

//Sorunun en yakın parçalarını bulur (RAG'in "retrieval" kısmı).
//1) Soru Gemini ile 768 sayıya çevrilir ("soru" olarak: RETRIEVAL_QUERY; parçalar "belge" olarak çevrilmişti)
//2) pgvector, yalnız bu videonun parçaları arasında kosinüs uzaklığına göre en yakınları sıralar (<=>)
//3) Benzerliği eşiğin altında kalanlar atılır: alakasız soruya "en az alakasız" parçayı vermemek için
@Service
public class TranscriptRetriever {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final double minScore;
    private final int maxChunks;

    public TranscriptRetriever(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate,
                               @Value("${qa.min-score}") double minScore,
                               @Value("${qa.max-chunks}") int maxChunks)
    {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.minScore = minScore;
        this.maxChunks = maxChunks;
    }

    public List<QuestionResponseDto.Source> findRelevant(Long videoId, String question)
    {
        float[] vector = embedQuestion(question);
        //pgvector'ın metin biçimi: "[0.1,0.2,...]"
        String literal = Arrays.toString(vector).replace(" ", "");

        //benzerlik = 1 - kosinüs uzaklığı (1 = aynı yön, 0 = alakasız)
        List<QuestionResponseDto.Source> nearest = jdbcTemplate.query("""
                SELECT content,
                       (metadata->>'start')::float8 AS start_s,
                       (metadata->>'end')::float8   AS end_s,
                       1 - (embedding <=> ?::vector) AS score
                FROM vector_store
                WHERE video_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, i) -> new QuestionResponseDto.Source(rs.getDouble("start_s"), rs.getDouble("end_s"),
                        rs.getString("content"), Math.round(rs.getDouble("score") * 1000) / 1000.0),
                literal, videoId, literal, maxChunks);

        return nearest.stream().filter(s -> s.score() >= minScore).toList();
    }

    //Zamana göre seçim (vektör araması yok): videonun başı, sonu ya da belli bir anı
    public List<QuestionResponseDto.Source> findByTime(Long videoId, QuestionIntent intent)
    {
        List<QuestionResponseDto.Source> all = jdbcTemplate.query("""
                SELECT content, (metadata->>'start')::float8 AS start_s, (metadata->>'end')::float8 AS end_s
                FROM vector_store
                WHERE video_id = ?
                ORDER BY start_s
                """,
                (rs, i) -> new QuestionResponseDto.Source(rs.getDouble("start_s"), rs.getDouble("end_s"), rs.getString("content"), null),
                videoId);
        if(all.isEmpty())
            return all;

        return switch(intent.type())
        {
            case BEGINNING -> all.subList(0, Math.min(2, all.size()));
            case END -> all.subList(Math.max(0, all.size() - 2), all.size());
            case AT_TIME -> around(all, intent.seconds());
            default -> List.of();
        };
    }

    //O anı kapsayan parça ve bir sonraki (örtüşme yüzünden iki parça kapsayabilir; ilkini alıp devamını ekliyoruz).
    //Video o kadar uzun değilse boş: "20. dakika" 10 dakikalık videoda yok.
    private static List<QuestionResponseDto.Source> around(List<QuestionResponseDto.Source> all, double seconds)
    {
        for(int i = 0; i < all.size(); i++)
        {
            if(all.get(i).end() >= seconds)
                return all.get(i).start() > seconds + 60 ? List.of() : all.subList(i, Math.min(i + 2, all.size()));
        }
        return List.of();
    }

    float[] embedQuestion(String question)
    {
        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model("gemini-embedding-001")
                .dimensions(768)
                .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY)
                .build();
        return embeddingModel.call(new EmbeddingRequest(List.of(question), options)).getResult().getOutput();
    }
}
