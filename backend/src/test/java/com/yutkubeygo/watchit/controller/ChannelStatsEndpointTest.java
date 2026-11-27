package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Stüdyo istatistikleri: sayılan görüntülenme bugünün çubuğuna yazılır; boş günler 0 olarak gelir (gerçek PostgreSQL, geri alınır)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChannelStatsEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User user(String name)
    {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.local");
        u.setPassword("x");
        return userRepository.saveAndFlush(u);
    }

    @Test
    void viewsShowUpOnTodayAndEmptyDaysAreZero() throws Exception
    {
        User owner = user("istatistik_testi_sahip");
        User viewer = user("istatistik_testi_izleyici");
        Channel channel = new Channel();
        channel.setChannelName("İstatistik testi kanalı");
        channel.setHandle("istatistik_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);
        Video video = new Video();
        video.setTitle("İstatistik videosu");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(600);
        videoRepository.saveAndFlush(video);

        mockMvc.perform(post("/videos/" + video.getId() + "/view").header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isNoContent());

        String bearer = "Bearer " + jwtService.generateToken(owner);
        mockMvc.perform(get("/channels/me/stats").param("days", "7").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(7)))
                .andExpect(jsonPath("$.daily[6].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.daily[6].views").value(1))
                .andExpect(jsonPath("$.daily[0].views").value(0))
                .andExpect(jsonPath("$.periodViews").value(1))
                .andExpect(jsonPath("$.videoCount").value(1));

        mockMvc.perform(get("/channels/me/stats").param("days", "5").header("Authorization", bearer)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/channels/me/stats").header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/channels/me/stats")).andExpect(status().isUnauthorized());
    }
}
