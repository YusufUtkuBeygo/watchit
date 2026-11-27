package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.dto.TranscriptSearchResultDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.service.TranscriptSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//Transkriptte kelime araması gerçek PostgreSQL'e karşı (düzenli ifade, kelime sınırı, unaccent). Sonunda geri alınır
@SpringBootTest
@Transactional
class TranscriptSearchQueryTest {

    @Autowired private TranscriptSearchService service;
    @Autowired private TranscriptSegmentRepository segmentRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Video many;
    private Video once;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUsername("transkript_arama_testi");
        user.setEmail("transkript_arama_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);
        Channel channel = new Channel();
        channel.setChannelName("Transkript arama kanalı");
        channel.setHandle("transkript_arama_kanali");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);

        many = video(channel, "QQT çok geçen");
        once = video(channel, "QQT bir kez geçen");
        segment(many, 10, "Zqxwyl ile başlıyoruz.");
        segment(many, 40, "Bir kez daha ZQXWYL dedik.");
        segment(many, 70, "Zqxwylçılar burada yok.");      //kelimenin parçası: sayılmaz
        segment(once, 5, "Şemsiye ve zqxwyl, ve c++ kodu.");
    }

    private Video video(Channel channel, String title)
    {
        Video v = new Video();
        v.setTitle(title);
        v.setChannel(channel);
        v.setCategory(categoryRepository.findAll().getFirst());
        v.setDurationInSeconds(100);
        return videoRepository.saveAndFlush(v);
    }

    private void segment(Video video, double start, String text)
    {
        TranscriptSegment s = new TranscriptSegment();
        s.setVideo(video);
        s.setStartSeconds(start);
        s.setEndSeconds(start + 5);
        s.setText(text);
        segmentRepository.saveAndFlush(s);
    }

    private List<TranscriptSearchResultDto> search(String q)
    {
        PageResponseDto<TranscriptSearchResultDto> page = service.search(q, PageRequest.of(0, 50));
        return page.items().stream().filter(r -> r.video().getTitle().startsWith("QQT")).toList();
    }

    @Test
    void wholeWordCaseInsensitiveMostHitsFirst()
    {
        List<TranscriptSearchResultDto> results = search("zqxwyl");
        assertThat(results).extracting(r -> r.video().getId()).containsExactly(many.getId(), once.getId());
        assertThat(results.getFirst().hits()).isEqualTo(2);
        assertThat(results.getFirst().matches()).extracting(TranscriptSearchResultDto.Match::startSeconds).containsExactly(10.0, 40.0);
    }

    @Test
    void turkishLettersAndRegexCharactersAreSafe()
    {
        assertThat(search("semsiye")).hasSize(1);
        assertThat(search("ŞEMSİYE")).hasSize(1);
        //Özel karakterler kaçırılır: "+" düzenli ifade anlamını yitirir, metindeki "c++ kodu" birebir bulunur
        assertThat(search("c++ kodu")).hasSize(1);
        assertThat(search("cc kodu")).isEmpty();
        assertThat(service.search(".*", PageRequest.of(0, 5))).isNull();     //3 karakterden kısa: 400
        assertThat(search("a.*b")).isEmpty();
    }
}
