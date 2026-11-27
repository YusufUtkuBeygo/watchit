package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

//Sayfalı video listesi ve süzgeçler gerçek PostgreSQL'e karşı (her test sonunda geri alınır)
@SpringBootTest
@Transactional
class VideoListQueryTest {

    @Autowired private VideoRepository videoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private WatchHistoryRepository watchHistoryRepository;

    private Channel channel;
    private Category category;
    private User viewer;

    @BeforeEach
    void setUp()
    {
        User owner = user("liste_testi_sahip");
        viewer = user("liste_testi_izleyici");

        channel = new Channel();
        channel.setChannelName("Liste testi kanalı");
        channel.setHandle("liste_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);

        category = categoryRepository.findAll().getFirst();
        //5 video: süreleri 30, 60, 90, 120, 300 sn; görüntülenme 5, 4, 3, 2, 1 (ilk eklenen en popüler, son eklenen en yeni)
        int[] durations = {30, 60, 90, 120, 300};
        for(int i = 0; i < durations.length; i++)
            save("LST video " + i, durations[i], 5L - i);
    }

    private User user(String name)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        return userRepository.saveAndFlush(user);
    }

    private void save(String title, int duration, long views)
    {
        Video video = new Video();
        video.setTitle(title);
        video.setChannel(channel);
        video.setCategory(category);
        video.setDurationInSeconds(duration);
        video = videoRepository.saveAndFlush(video);
        //viewCount güncellenemez sütun (@Column(updatable = false)); sayaç sorgusuyla artırılır
        for(long i = 0; i < views; i++)
            videoRepository.incrementViewCount(video.getId());
    }

    private Page<Video> page(Specification<Video> spec, int page, int size, org.springframework.data.domain.Sort sort)
    {
        return videoRepository.findAll(spec, PageRequest.of(page, size, sort));
    }

    @Test
    void pagesSplitTheChannelVideosWithoutOverlap()
    {
        Specification<Video> spec = VideoSpecs.inChannel(channel.getId());
        Page<Video> first = page(spec, 0, 2, VideoService.NEWEST);
        Page<Video> second = page(spec, 1, 2, VideoService.NEWEST);
        Page<Video> last = page(spec, 2, 2, VideoService.NEWEST);

        assertThat(first.getContent()).extracting(Video::getTitle).containsExactly("LST video 4", "LST video 3");
        assertThat(second.getContent()).extracting(Video::getTitle).containsExactly("LST video 2", "LST video 1");
        assertThat(last.getContent()).extracting(Video::getTitle).containsExactly("LST video 0");
        assertThat(first.hasNext()).isTrue();
        assertThat(last.hasNext()).isFalse();
        assertThat(first.getTotalElements()).isEqualTo(5);
    }

    @Test
    void noFiltersListsEverything()
    {
        assertThat(page(VideoSpecs.allOf(VideoSpecs.inChannel(null), VideoSpecs.inCategory(null), VideoSpecs.maxDuration(null)),
                0, 50, VideoService.NEWEST).getTotalElements()).isEqualTo(videoRepository.count());
    }

    @Test
    void popularSortsByViews()
    {
        assertThat(page(VideoSpecs.inChannel(channel.getId()), 0, 3, VideoService.POPULAR).getContent())
                .extracting(Video::getTitle).containsExactly("LST video 0", "LST video 1", "LST video 2");
    }

    @Test
    void maxDurationKeepsShortVideos()
    {
        Specification<Video> spec = VideoSpecs.allOf(VideoSpecs.inChannel(channel.getId()), VideoSpecs.inCategory(null), VideoSpecs.maxDuration(90));
        assertThat(page(spec, 0, 10, VideoService.NEWEST).getContent())
                .extracting(Video::getDurationInSeconds).containsExactly(90, 60, 30);
    }

    @Test
    void subscriptionFeedShowsOnlySubscribedChannels()
    {
        assertThat(page(VideoSpecs.subscribedBy(viewer.getId()), 0, 10, VideoService.NEWEST).getContent()).isEmpty();

        Subscription subscription = new Subscription();
        subscription.setSubscriber(viewer);
        subscription.setChannel(channel);
        subscriptionRepository.saveAndFlush(subscription);

        assertThat(page(VideoSpecs.subscribedBy(viewer.getId()), 0, 10, VideoService.NEWEST).getTotalElements()).isEqualTo(5);
    }

    @Test
    void forYouPutsSubscribedChannelAboveAndWatchedBelow()
    {
        //Bu testin kanalındaki en popüler video (5 görüntülenme): abone olunca kanal +3 puan alır
        Subscription subscription = new Subscription();
        subscription.setSubscriber(viewer);
        subscription.setChannel(channel);
        subscriptionRepository.saveAndFlush(subscription);

        java.util.List<Long> mine = videoRepository.findForYou(viewer.getId(), category.getId(), PageRequest.of(0, 50)).stream()
                .filter(v -> v.getChannel().getId() == channel.getId()).map(Video::getId).toList();
        assertThat(mine).hasSize(5);
        Long top = mine.getFirst();

        //En üstteki videoyu bitirince (-5) artık ilk sırada değil
        com.yutkubeygo.watchit.entity.WatchHistory h = new com.yutkubeygo.watchit.entity.WatchHistory();
        h.setUser(viewer);
        h.setVideo(videoRepository.findById(top).orElseThrow());
        h.setIsCompleted(true);
        h.setWatchedSeconds(30);
        h.setLastWatchedAt(java.time.LocalDateTime.now());
        watchHistoryRepository.saveAndFlush(h);

        java.util.List<Long> after = videoRepository.findForYou(viewer.getId(), category.getId(), PageRequest.of(0, 50)).stream()
                .filter(v -> v.getChannel().getId() == channel.getId()).map(Video::getId).toList();
        assertThat(after.getFirst()).isNotEqualTo(top);
        assertThat(after.getLast()).isEqualTo(top);
    }

    @Test
    void forYouWorksWithoutLogin()
    {
        assertThat(videoRepository.findForYou(null, null, PageRequest.of(0, 5)).getTotalElements()).isEqualTo(videoRepository.count());
    }
}
