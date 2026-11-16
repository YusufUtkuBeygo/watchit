package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.TranscriptSegment;

import java.util.ArrayList;
import java.util.List;

//Whisper'ın cümlelerini 30-60 saniyelik parçalara toplar (her parça ayrı vektöre çevrilir).
//Neden parça: tek cümle çok az bilgi taşır ("Evet, aynen."), bütün video ise tek vektörde "sulanır".
//Örtüşme: bir parçanın son ~10 saniyesi bir sonrakinin başına da konur; konu tam sınıra denk gelirse ortadan bölünmez.
public final class TranscriptChunker {

    static final double TARGET_SECONDS = 45;   //bu süreye ulaşınca parça kapanır
    static final double MAX_SECONDS = 60;      //bir sonraki cümle bunu aşacaksa parça önceden kapanır
    static final double OVERLAP_SECONDS = 10;  //bir sonraki parçaya taşınan kuyruk

    public record Chunk(double start, double end, String text) {}

    private TranscriptChunker() {}

    public static List<Chunk> chunk(List<TranscriptSegment> segments)
    {
        List<Chunk> chunks = new ArrayList<>();
        List<TranscriptSegment> current = new ArrayList<>();
        int newInCurrent = 0;   //örtüşmeden gelmeyen, bu parçaya ilk kez giren cümle sayısı

        for(TranscriptSegment segment : segments)
        {
            //Bu cümle parçayı 60 saniyenin üstüne taşıyacaksa önce parçayı kapat
            if(newInCurrent > 0 && segment.getEndSeconds() - current.getFirst().getStartSeconds() > MAX_SECONDS)
            {
                chunks.add(toChunk(current));
                current = overlapTail(current);
                newInCurrent = 0;
                //Araya uzun bir sessizlik girdiyse kuyruğu taşıma: parça boşluğun öncesine uzayıp zamanı yanıltmasın
                if(!current.isEmpty() && segment.getStartSeconds() - current.getLast().getEndSeconds() > OVERLAP_SECONDS)
                    current.clear();
            }

            current.add(segment);
            newInCurrent++;

            if(segment.getEndSeconds() - current.getFirst().getStartSeconds() >= TARGET_SECONDS)
            {
                chunks.add(toChunk(current));
                current = overlapTail(current);
                newInCurrent = 0;
            }
        }

        //Kalan cümleler (yalnız örtüşmeden gelenler kaldıysa onlar zaten önceki parçada var)
        if(newInCurrent > 0)
            chunks.add(toChunk(current));

        return chunks;
    }

    //Parçanın son ~10 saniyesindeki cümleler (parçanın tamamı değil: en az bir cümle dışarıda kalır)
    private static List<TranscriptSegment> overlapTail(List<TranscriptSegment> chunk)
    {
        double end = chunk.getLast().getEndSeconds();
        List<TranscriptSegment> tail = new ArrayList<>();
        for(int i = chunk.size() - 1; i >= 1; i--)
        {
            TranscriptSegment s = chunk.get(i);
            if(end - s.getStartSeconds() > OVERLAP_SECONDS)
                break;
            tail.addFirst(s);
        }
        return tail;
    }

    private static Chunk toChunk(List<TranscriptSegment> segments)
    {
        StringBuilder text = new StringBuilder();
        for(TranscriptSegment s : segments)
        {
            if(!text.isEmpty())
                text.append(' ');
            text.append(s.getText());
        }
        return new Chunk(segments.getFirst().getStartSeconds(), segments.getLast().getEndSeconds(), text.toString());
    }
}
