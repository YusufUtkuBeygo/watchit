package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.TranscriptSegment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoTranscript;
import com.yutkubeygo.watchit.enums.TranscriptStatus;
import com.yutkubeygo.watchit.exception.ConflictException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.TranscriptSegmentRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.VideoTranscriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//TranscriptService gerçek PostgreSQL'e karşı: durum okuma, kim başlatabilir, çok uzun video, çift istek.
//Arka plan işçisi sahte (@MockitoBean): Whisper'a gidilmez. @Transactional: test sonunda her şey geri alınır.
@SpringBootTest
@Transactional
class TranscriptServiceTest {

    @Autowired private TranscriptService transcriptService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoTranscriptRepository transcriptRepository;
    @Autowired private TranscriptSegmentRepository segmentRepository;

    @MockitoBean
    private TranscriptWorker worker;

    private User owner;
    private User other;
    private Video video;

    @BeforeEach
    void setUp()
    {
        owner = saveUser("tr_testi_sahip");
        other = saveUser("tr_testi_diger");

        Channel channel = new Channel();
        channel.setChannelName("Transkript testi kanalı");
        channel.setHandle("tr_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Deneme");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(600);
        video.setVideoUrl("http://localhost:9000/watchit-images/deneme.mp4");
        videoRepository.saveAndFlush(video);
    }

    private User saveUser(String name)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        return userRepository.saveAndFlush(user);
    }

    @Test
    void notStartedWhenNoTranscript()
    {
        assertThat(transcriptService.getTranscript(video.getId()).status()).isEqualTo("NOT_STARTED");
        assertThat(transcriptService.getTranscript(-1L)).isNull();
    }

    @Test
    void ownerCanRequestOthersCannot()
    {
        assertThatThrownBy(() -> transcriptService.requestTranscript(video.getId(), other.getId()))
                .isInstanceOf(ForbiddenException.class);

        TranscriptResponseDto result = transcriptService.requestTranscript(video.getId(), owner.getId());
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(transcriptService.requestTranscript(-1L, owner.getId())).isNull();
    }

    @Test
    void secondRequestWhilePendingIsConflict()
    {
        transcriptService.requestTranscript(video.getId(), owner.getId());

        assertThatThrownBy(() -> transcriptService.requestTranscript(video.getId(), owner.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void videoLongerThanOneHourFailsWithoutWhisper()
    {
        video.setDurationInSeconds(3601);
        videoRepository.saveAndFlush(video);

        transcriptService.start(video);

        TranscriptResponseDto result = transcriptService.getTranscript(video.getId());
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).contains("60 dakikadan uzun");
    }

    @Test
    void videoWithoutFileIsSkipped()
    {
        video.setVideoUrl(null);
        videoRepository.saveAndFlush(video);

        transcriptService.start(video);

        assertThat(transcriptService.getTranscript(video.getId()).status()).isEqualTo("NOT_STARTED");
    }

    @Test
    void readyTranscriptReturnsSegmentsInOrder()
    {
        VideoTranscript transcript = new VideoTranscript();
        transcript.setVideo(video);
        transcript.setStatus(TranscriptStatus.READY);
        transcript.setLanguage("tr");
        transcriptRepository.saveAndFlush(transcript);
        for(double[] t : new double[][]{{5, 9}, {0, 5}})
        {
            TranscriptSegment s = new TranscriptSegment();
            s.setVideo(video);
            s.setStartSeconds(t[0]);
            s.setEndSeconds(t[1]);
            s.setText(t[0] == 0 ? "Merhaba." : "Bugün menemen yapacağız.");
            segmentRepository.saveAndFlush(s);
        }

        TranscriptResponseDto result = transcriptService.getTranscript(video.getId());
        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.language()).isEqualTo("tr");
        assertThat(result.segments()).extracting(TranscriptResponseDto.Segment::text)
                .containsExactly("Merhaba.", "Bugün menemen yapacağız.");
    }
}
