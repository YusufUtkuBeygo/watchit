package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//Dönüştürme sorguları gerçek PostgreSQL'e karşı: dosya yalnız video hâlâ aynı dosyayı gösteriyorsa değişir,
//başlık gibi diğer alanlara dokunulmaz. @Transactional: test sonunda her şey geri alınır.
@SpringBootTest
@Transactional
class VideoProcessingQueryTest {

    private static final String OLD = "http://localhost:9000/watchit-images/eski.webm";
    private static final String NEW = "http://localhost:9000/watchit-images/islenmis/yeni.mp4";

    @Autowired private VideoRepository videoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager entityManager;

    private Video video;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUsername("donusturme_testi");
        user.setEmail("donusturme_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);

        Channel channel = new Channel();
        channel.setChannelName("Dönüştürme testi");
        channel.setHandle("donusturme_testi");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Sahibinin başlığı");
        video.setVideoUrl(OLD);
        video.setDurationInSeconds(7);
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setProcessingStatus(VideoProcessingStatus.PENDING);
        videoRepository.saveAndFlush(video);
    }

    //UPDATE sorgusu veritabanına gider; aynı transaction'daki önbellek eski kalır, temizleyip yeniden okuyoruz
    private Video reload()
    {
        entityManager.flush();
        entityManager.clear();
        return videoRepository.findById(video.getId()).orElseThrow();
    }

    @Test
    void takesJobOnlyWhenPending()
    {
        assertThat(videoRepository.changeProcessingStatus(video.getId(), VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).isEqualTo(1);
        //İkinci kez alınamaz
        assertThat(videoRepository.changeProcessingStatus(video.getId(), VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING)).isZero();
        assertThat(reload().getProcessingStatus()).isEqualTo(VideoProcessingStatus.PROCESSING);
    }

    @Test
    void swapsFileAndDurationButKeepsOtherFields()
    {
        int changed = videoRepository.swapProcessedFile(video.getId(), OLD, NEW, 8, VideoProcessingStatus.READY);

        Video v = reload();
        assertThat(changed).isEqualTo(1);
        assertThat(v.getVideoUrl()).isEqualTo(NEW);
        assertThat(v.getDurationInSeconds()).isEqualTo(8);
        assertThat(v.getProcessingStatus()).isEqualTo(VideoProcessingStatus.READY);
        assertThat(v.getTitle()).isEqualTo("Sahibinin başlığı");
    }

    @Test
    void keepsDurationWhenUnknown()
    {
        videoRepository.swapProcessedFile(video.getId(), OLD, NEW, null, VideoProcessingStatus.READY);
        assertThat(reload().getDurationInSeconds()).isEqualTo(7);
    }

    @Test
    void doesNotSwapWhenFileChangedMeanwhile()
    {
        int changed = videoRepository.swapProcessedFile(video.getId(), "http://localhost:9000/watchit-images/baska.mp4", NEW, 8, VideoProcessingStatus.READY);

        assertThat(changed).isZero();
        assertThat(reload().getVideoUrl()).isEqualTo(OLD);
    }

    @Test
    void failOnlyTouchesSameFile()
    {
        assertThat(videoRepository.failProcessing(video.getId(), "http://localhost:9000/watchit-images/baska.mp4", VideoProcessingStatus.FAILED, "x")).isZero();
        assertThat(videoRepository.failProcessing(video.getId(), OLD, VideoProcessingStatus.FAILED, "Video dönüştürülemedi")).isEqualTo(1);

        Video v = reload();
        assertThat(v.getProcessingStatus()).isEqualTo(VideoProcessingStatus.FAILED);
        assertThat(v.getProcessingError()).isEqualTo("Video dönüştürülemedi");
    }

    @Test
    void findsUnfinishedJobsAndFileUsers()
    {
        assertThat(videoRepository.findByProcessingStatusIn(List.of(VideoProcessingStatus.PENDING)))
                .extracting(Video::getId).contains(video.getId());
        assertThat(videoRepository.existsByVideoUrl(OLD)).isTrue();
        assertThat(videoRepository.existsByVideoUrl(NEW)).isFalse();
    }
}
