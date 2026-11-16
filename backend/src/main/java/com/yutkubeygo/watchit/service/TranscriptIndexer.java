package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.TranscriptSegment;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

//Transkript parçalarını vektöre çevirip vector_store'a yazar (2c).
//VectorStore (Spring AI) her parçanın metnini Gemini'ye gönderir, dönen 768 sayıyı metinle birlikte kaydeder.
//Parçalar "belge" olarak vektöre çevrilir (task-type=RETRIEVAL_DOCUMENT, application.properties).
@Service
public class TranscriptIndexer {

    private final VectorStore vectorStore;

    public TranscriptIndexer(VectorStore vectorStore)
    {
        this.vectorStore = vectorStore;
    }

    //Videonun eski parçalarını siler, yenilerini yazar. Döndürdüğü: kaç parça yazıldı.
    public int index(Long videoId, List<TranscriptSegment> segments)
    {
        deleteForVideo(videoId);

        List<Document> documents = TranscriptChunker.chunk(segments).stream()
                .map(c -> new Document(c.text(), Map.of(
                        "videoId", videoId,
                        "start", c.start(),
                        "end", c.end())))
                .toList();

        if(!documents.isEmpty())
            vectorStore.add(documents);

        return documents.size();
    }

    //Video silinince veritabanı kendisi siler (V12), bu metot yeniden vektörlemede eskileri temizlemek için
    public void deleteForVideo(Long videoId)
    {
        vectorStore.delete("videoId == " + videoId);
    }
}
