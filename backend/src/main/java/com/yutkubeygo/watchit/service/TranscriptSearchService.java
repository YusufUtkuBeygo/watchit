package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.dto.TranscriptSearchResultDto;
import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//Transkriptte kelime araması: "bu kelime hangi videoda, kaçıncı saniyede söyleniyor?"
//Bilerek anlamsal (vektör) değil: anlam araması "teleskop" yazınca uzayla ilgili her şeyi getirir; burada kelime ya geçer ya geçmez.
//Bedeli: dil çevirisi yok ("teleskop" İngilizce videodaki "telescope"u bulmaz).
@Service
public class TranscriptSearchService {

    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 100;
    static final int MATCHES_PER_VIDEO = 3;

    private final TranscriptSegmentRepository segmentRepository;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    public TranscriptSearchService(TranscriptSegmentRepository segmentRepository, VideoRepository videoRepository, VideoMapper videoMapper)
    {
        this.segmentRepository = segmentRepository;
        this.videoRepository = videoRepository;
        this.videoMapper = videoMapper;
    }

    //Kelime 3-100 karakter değilse null (controller 400)
    public PageResponseDto<TranscriptSearchResultDto> search(String query, Pageable pageable)
    {
        String text = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        if(text.length() < MIN_LENGTH || text.length() > MAX_LENGTH)
            return null;

        String pattern = toPattern(text);
        Page<Object[]> page = segmentRepository.searchVideos(pattern, pageable);
        List<TranscriptSearchResultDto> items = new ArrayList<>();
        for(Object[] row : page.getContent())
        {
            Long videoId = ((Number) row[0]).longValue();
            long hits = ((Number) row[1]).longValue();
            videoRepository.findById(videoId).ifPresent(video -> items.add(new TranscriptSearchResultDto(
                    videoMapper.toDto(video), hits,
                    segmentRepository.findMatches(videoId, pattern, MATCHES_PER_VIDEO).stream()
                            .map(s -> new TranscriptSearchResultDto.Match(s.getStartSeconds(), s.getText()))
                            .toList())));
        }
        return PageResponseDto.of(page, items);
    }

    //Kullanıcının yazdığı düz metindeki düzenli ifade karakterleri (. * ? ( ) [ ] ...) kaçırılır:
    //"c++" ya da "a.b" yazan biri sorgu dilini bozamasın. Kelime sınırı, küçük harf ve aksansızlaştırma SQL'de eklenir
    static String toPattern(String text)
    {
        StringBuilder escaped = new StringBuilder();
        for(char c : text.toCharArray())
        {
            if("\\.^$*+?()[]{}|".indexOf(c) >= 0)
                escaped.append('\\');
            escaped.append(c);
        }
        return escaped.toString();
    }
}
