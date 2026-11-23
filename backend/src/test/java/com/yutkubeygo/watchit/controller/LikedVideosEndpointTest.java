package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoLike;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoLikeRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//GET /videos/liked: giriş ister (açık GET /videos/** kuralına rağmen), yalnız benim beğendiklerim, en son beğenilen önce.
//Gerçek PostgreSQL; @Transactional: test sonunda eklenen her şey geri alınır.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LikedVideosEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoLikeRepository videoLikeRepository;

    private User me;
    private User other;
    private Video first;
    private Video second;

    @BeforeEach
    void setUp()
    {
        me = saveUser("begeni_testi_ben");
        other = saveUser("begeni_testi_diger");

        Channel channel = new Channel();
        channel.setChannelName("Beğeni testi kanalı");
        channel.setHandle("begeni_testi_kanali");
        channel.setOwner(other);
        channelRepository.saveAndFlush(channel);

        first = saveVideo(channel, "İlk beğendiğim");
        second = saveVideo(channel, "Sonra beğendiğim");
        Video notMine = saveVideo(channel, "Başkasının beğendiği");

        like(me, first);
        like(me, second);
        like(other, notMine);
    }

    private User saveUser(String name)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        return userRepository.saveAndFlush(user);
    }

    private Video saveVideo(Channel channel, String title)
    {
        Video video = new Video();
        video.setTitle(title);
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(10);
        return videoRepository.saveAndFlush(video);
    }

    private void like(User user, Video video)
    {
        VideoLike like = new VideoLike();
        like.setUser(user);
        like.setVideo(video);
        videoLikeRepository.saveAndFlush(like);
    }

    private String bearer(User user)
    {
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void withoutLoginIs401() throws Exception
    {
        mockMvc.perform(get("/videos/liked")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyMyLikesNewestFirst() throws Exception
    {
        mockMvc.perform(get("/videos/liked").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[1].id").value(first.getId()));
    }

    @Test
    void emptyWhenNothingLiked() throws Exception
    {
        User nobody = saveUser("begeni_testi_bos");
        mockMvc.perform(get("/videos/liked").header("Authorization", bearer(nobody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void videoByIdStillOpenWithoutLogin() throws Exception
    {
        //Yeni kural yalnız /videos/liked'i kapattı, tek video okuma hâlâ herkese açık
        mockMvc.perform(get("/videos/" + first.getId())).andExpect(status().isOk());
    }
}
