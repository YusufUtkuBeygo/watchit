package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ClipRequestDto;
import com.yutkubeygo.watchit.dto.ClipResponseDto;
import com.yutkubeygo.watchit.dto.CourseResponseDto;
import com.yutkubeygo.watchit.dto.PlaylistRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Playlist;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.WatchHistory;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.PlaylistRepository;
import com.yutkubeygo.watchit.repository.PlaylistVideoRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.WatchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//Klipler ve kurslar gerçek PostgreSQL'e karşı. @Transactional: test sonunda her şey geri alınır.
@SpringBootTest
@Transactional
class ClipAndCourseServiceTest {

    @Autowired private ClipService clipService;
    @Autowired private CourseService courseService;
    @Autowired private PlaylistService playlistService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PlaylistRepository playlistRepository;
    @Autowired private PlaylistVideoRepository playlistVideoRepository;
    @Autowired private WatchHistoryRepository watchHistoryRepository;

    private User owner;
    private User viewer;
    private Video video;

    @BeforeEach
    void setUp()
    {
        owner = saveUser("klip_testi_sahip");
        viewer = saveUser("klip_testi_izleyici");

        Channel channel = new Channel();
        channel.setChannelName("Klip testi kanalı");
        channel.setHandle("klip_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Deneme");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(120);
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

    private ClipRequestDto clip(Long videoId, int start, int end)
    {
        ClipRequestDto request = new ClipRequestDto();
        request.setVideoId(videoId);
        request.setTitle("  Güzel an  ");
        request.setStartSeconds(start);
        request.setEndSeconds(end);
        return request;
    }

    @Test
    void validClipIsSavedAndListedForItsOwner()
    {
        ClipResponseDto saved = clipService.createClip(viewer.getId(), clip(video.getId(), 10, 25));

        assertThat(saved.title()).isEqualTo("Güzel an");
        assertThat(saved.video().getId()).isEqualTo(video.getId());
        assertThat(clipService.getMyClips(viewer.getId())).extracting(ClipResponseDto::id).containsExactly(saved.id());
        assertThat(clipService.getMyClips(owner.getId())).isEmpty();
        assertThat(clipService.getClip(saved.id())).isNotNull();
    }

    @Test
    void clipLengthAndRangeAreChecked()
    {
        //5 sn'den kısa, 60 sn'den uzun, videonun sonunu geçen
        assertThatThrownBy(() -> clipService.createClip(viewer.getId(), clip(video.getId(), 0, 4))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> clipService.createClip(viewer.getId(), clip(video.getId(), 0, 61))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> clipService.createClip(viewer.getId(), clip(video.getId(), 110, 125))).isInstanceOf(BadRequestException.class);
        //Sınırlar dahil: tam 5 ve tam 60 sn geçerli
        assertThat(clipService.createClip(viewer.getId(), clip(video.getId(), 0, 5))).isNotNull();
        assertThat(clipService.createClip(viewer.getId(), clip(video.getId(), 60, 120))).isNotNull();
        //Video yoksa null (controller 404)
        assertThat(clipService.createClip(viewer.getId(), clip(-1L, 0, 10))).isNull();
    }

    @Test
    void onlyOwnerCanDeleteClip()
    {
        ClipResponseDto saved = clipService.createClip(viewer.getId(), clip(video.getId(), 0, 10));

        assertThatThrownBy(() -> clipService.deleteClip(saved.id(), owner.getId())).isInstanceOf(ForbiddenException.class);
        assertThat(clipService.deleteClip(saved.id(), viewer.getId())).isTrue();
        assertThat(clipService.deleteClip(saved.id(), viewer.getId())).isFalse();
    }

    @Test
    void courseIsAlwaysPublicAndShowsProgress()
    {
        PlaylistRequestDto request = new PlaylistRequestDto();
        request.setTitle("Test kursu");
        request.setIsPublic(false);
        request.setIsCourse(true);
        PlaylistResponseDto created = playlistService.createPlaylist(request, owner.getId());
        assertThat(created.getIsCourse()).isTrue();
        assertThat(created.getIsPublic()).isTrue();

        //Videosu olmayan kurs listede yok
        assertThat(courseIds(viewer.getId())).doesNotContain(created.getId());

        Playlist playlist = playlistRepository.findById(created.getId()).orElseThrow();
        PlaylistVideo lesson = new PlaylistVideo();
        lesson.setPlaylist(playlist);
        lesson.setVideo(video);
        lesson.setOrderIndex(0);
        playlistVideoRepository.saveAndFlush(lesson);

        CourseResponseDto before = course(viewer.getId(), created.getId());
        assertThat(before.videoCount()).isEqualTo(1);
        assertThat(before.watchedCount()).isZero();
        assertThat(before.firstVideoId()).isEqualTo(video.getId());

        WatchHistory history = new WatchHistory();
        history.setUser(viewer);
        history.setVideo(video);
        history.setWatchedSeconds(30);
        history.setIsCompleted(false);
        watchHistoryRepository.saveAndFlush(history);

        assertThat(course(viewer.getId(), created.getId()).watchedCount()).isEqualTo(1);
        //Girişsiz istekte ilerleme sorulmaz
        assertThat(course(null, created.getId()).watchedCount()).isZero();
    }

    @Test
    void playlistWithoutCourseFlagIsNotACourse()
    {
        PlaylistRequestDto request = new PlaylistRequestDto();
        request.setTitle("Normal liste");
        request.setIsPublic(false);
        PlaylistResponseDto created = playlistService.createPlaylist(request, owner.getId());

        assertThat(created.getIsCourse()).isFalse();
        assertThat(created.getIsPublic()).isFalse();
    }

    private List<Long> courseIds(Long userId)
    {
        return courseService.getCourses(userId).stream().map(CourseResponseDto::id).toList();
    }

    private CourseResponseDto course(Long userId, Long id)
    {
        return courseService.getCourses(userId).stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }
}
