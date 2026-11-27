package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//"Daha sonra izle": giriş ister, ekle (201, tekrar 200), liste sırası, video cevabında savedForLater, çıkar (204, yoksa 404),
//başkasının listesi görünmez. Gerçek PostgreSQL; @Transactional: test sonunda her şey geri alınır.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WatchLaterEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User me;
    private User other;
    private Video first;
    private Video second;

    @BeforeEach
    void setUp()
    {
        me = saveUser("sonra_testi_ben");
        other = saveUser("sonra_testi_diger");

        Channel channel = new Channel();
        channel.setChannelName("Sonra izle testi");
        channel.setHandle("sonra_testi_kanali");
        channel.setOwner(other);
        channelRepository.saveAndFlush(channel);

        first = saveVideo(channel, "Önce eklenen");
        second = saveVideo(channel, "Sonra eklenen");
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

    private String bearer(User user)
    {
        return "Bearer " + jwtService.generateToken(user);
    }

    private void add(User user, Video video, int expectedStatus) throws Exception
    {
        mockMvc.perform(post("/watchLater").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":" + video.getId() + "}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void withoutLoginIs401() throws Exception
    {
        mockMvc.perform(get("/watchLater")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/watchLater").contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addTwiceIsNotAnErrorAndListIsNewestFirst() throws Exception
    {
        add(me, first, 201);
        add(me, second, 201);
        add(me, first, 200);

        mockMvc.perform(get("/watchLater").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(second.getId()))
                .andExpect(jsonPath("$.items[1].id").value(first.getId()));
    }

    @Test
    void videoResponseTellsIfSaved() throws Exception
    {
        add(me, first, 201);

        mockMvc.perform(get("/videos/" + first.getId()).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.savedForLater").value(true));
        mockMvc.perform(get("/videos/" + first.getId()).header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.savedForLater").value(false));
        mockMvc.perform(get("/videos/" + first.getId()))
                .andExpect(jsonPath("$.savedForLater").value(false));
    }

    @Test
    void removeThen404() throws Exception
    {
        add(me, first, 201);
        mockMvc.perform(delete("/watchLater").param("videoId", first.getId().toString()).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/watchLater").param("videoId", first.getId().toString()).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUsersListIsSeparate() throws Exception
    {
        add(me, first, 201);
        mockMvc.perform(get("/watchLater").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.items", hasSize(0)));
        //Başkası benim kaydımı silemez: onun listesinde yok
        mockMvc.perform(delete("/watchLater").param("videoId", first.getId().toString()).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingVideoIs404() throws Exception
    {
        mockMvc.perform(post("/watchLater").header("Authorization", bearer(me))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"videoId\":999999999}"))
                .andExpect(status().isNotFound());
    }
}
