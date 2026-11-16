package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.TranscriptSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//TranscriptChunker: cümleleri 30-60 sn'lik, ~10 sn örtüşen parçalara toplar; sessizlik boşluğunda örtüşme taşımaz.
class TranscriptChunkerTest {

    private static TranscriptSegment seg(double start, double end, String text)
    {
        TranscriptSegment s = new TranscriptSegment();
        s.setStartSeconds(start);
        s.setEndSeconds(end);
        s.setText(text);
        return s;
    }

    //Her 5 saniyede bir cümle: "c0", "c1", ...
    private static List<TranscriptSegment> everyFiveSeconds(int seconds)
    {
        List<TranscriptSegment> list = new ArrayList<>();
        for(int t = 0; t < seconds; t += 5)
            list.add(seg(t, t + 5, "c" + (t / 5)));
        return list;
    }

    @Test
    void chunksAreAtMostSixtySecondsAndOverlap()
    {
        List<TranscriptChunker.Chunk> chunks = TranscriptChunker.chunk(everyFiveSeconds(300));

        assertThat(chunks).hasSizeGreaterThan(5);
        for(int i = 0; i < chunks.size(); i++)
        {
            TranscriptChunker.Chunk c = chunks.get(i);
            assertThat(c.end() - c.start()).isLessThanOrEqualTo(60);
            if(i > 0)
                assertThat(c.start()).isLessThan(chunks.get(i - 1).end());   //örtüşme var
        }
        assertThat(chunks.getFirst().start()).isEqualTo(0);
        assertThat(chunks.getLast().end()).isEqualTo(300);
    }

    @Test
    void firstChunkClosesAtTargetAndNextStartsTenSecondsEarlier()
    {
        List<TranscriptChunker.Chunk> chunks = TranscriptChunker.chunk(everyFiveSeconds(120));

        assertThat(chunks.get(0).start()).isEqualTo(0);
        assertThat(chunks.get(0).end()).isEqualTo(45);
        assertThat(chunks.get(1).start()).isEqualTo(35);
        assertThat(chunks.get(0).text()).startsWith("c0 c1 c2");
    }

    @Test
    void everySentenceIsInSomeChunk()
    {
        List<TranscriptSegment> segments = everyFiveSeconds(200);
        List<TranscriptChunker.Chunk> chunks = TranscriptChunker.chunk(segments);

        for(TranscriptSegment s : segments)
            assertThat(chunks).anyMatch(c -> (" " + c.text() + " ").contains(" " + s.getText() + " "));
    }

    @Test
    void longSilenceDoesNotCarryOverlap()
    {
        List<TranscriptChunker.Chunk> chunks = TranscriptChunker.chunk(List.of(
                seg(0, 5, "önce"), seg(5, 10, "biraz daha"),
                seg(200, 205, "uzun sessizlikten sonra")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1).start()).isEqualTo(200);
        assertThat(chunks.get(1).text()).isEqualTo("uzun sessizlikten sonra");
    }

    @Test
    void singleLongSentenceIsItsOwnChunk()
    {
        assertThat(TranscriptChunker.chunk(List.of(seg(0, 70, "çok uzun bir cümle"))))
                .containsExactly(new TranscriptChunker.Chunk(0, 70, "çok uzun bir cümle"));
    }

    @Test
    void emptyTranscriptHasNoChunks()
    {
        assertThat(TranscriptChunker.chunk(List.of())).isEmpty();
    }
}
