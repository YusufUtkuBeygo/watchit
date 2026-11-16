package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.TranscriptSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

//TranscriptIndexer: önce videonun eski parçalarını siler, sonra her parçayı videoId/start/end bilgisiyle yazar.
//Gerçek Gemini yerine sahte VectorStore kullanılır.
@ExtendWith(MockitoExtension.class)
class TranscriptIndexerTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private TranscriptIndexer indexer;

    private static TranscriptSegment seg(double start, double end, String text)
    {
        TranscriptSegment s = new TranscriptSegment();
        s.setStartSeconds(start);
        s.setEndSeconds(end);
        s.setText(text);
        return s;
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletesOldThenAddsChunksWithMetadata()
    {
        int count = indexer.index(5L, List.of(seg(0, 20, "Merhaba."), seg(20, 50, "Bugün menemen yapacağız.")));

        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).delete("videoId == 5");
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        order.verify(vectorStore).add(captor.capture());

        assertThat(count).isEqualTo(1);
        Document doc = captor.getValue().getFirst();
        assertThat(doc.getText()).isEqualTo("Merhaba. Bugün menemen yapacağız.");
        assertThat(doc.getMetadata()).containsEntry("videoId", 5L).containsEntry("start", 0.0).containsEntry("end", 50.0);
    }

    @Test
    void emptyTranscriptOnlyDeletes()
    {
        assertThat(indexer.index(5L, List.of())).isZero();

        verify(vectorStore).delete("videoId == 5");
        verify(vectorStore, never()).add(anyList());
    }
}
