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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Yükleme kotası (gerçek PostgreSQL ve MinIO, sonunda geri alınır). Sınırlar testte küçük:
//tek dosya 1000 bayt, toplam video 3000 bayt, günde 2500 bayt
@SpringBootTest(properties = {
        "upload.max-file-bytes=1000",
        "upload.quota.total-bytes=3000",
        "upload.quota.daily-bytes=2500"
})
@AutoConfigureMockMvc
@Transactional
class UploadQuotaEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User user;
    private Channel channel;
    private String bearer;

    @BeforeEach
    void setUp()
    {
        user = new User();
        user.setUsername("kota_testi");
        user.setEmail("kota_testi@test.local");
        user.setPassword("x");
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
        channel = new Channel();
        channel.setChannelName("Kota testi kanalı");
        channel.setHandle("kota_testi_kanali");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);
        bearer = "Bearer " + jwtService.generateToken(user);
    }

    private ResultActions ask(String contentType, long size) throws Exception
    {
        return mockMvc.perform(post("/api/files/upload-url").header("Authorization", bearer)
                .param("fileName", "deneme.bin").param("contentType", contentType).param("fileSize", String.valueOf(size)));
    }

    private void existingVideo(long sizeBytes)
    {
        Video video = new Video();
        video.setTitle("Kota videosu");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(10);
        video.setSizeBytes(sizeBytes);
        videoRepository.saveAndFlush(video);
    }

    @Test
    void sizeMustBeBetweenOneByteAndMaxFile() throws Exception
    {
        ask("video/mp4", 0).andExpect(status().isBadRequest());
        ask("video/mp4", 1001).andExpect(status().isBadRequest());
        //Boyut verilmezse de 400 (eski istemci)
        mockMvc.perform(post("/api/files/upload-url").header("Authorization", bearer)
                .param("fileName", "a.mp4").param("contentType", "video/mp4")).andExpect(status().isBadRequest());
    }

    @Test
    void uploadGoesToUploadFolder() throws Exception
    {
        ask("video/mp4", 500).andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl", containsString("/yuklemeler/")));
    }

    @Test
    void totalQuotaCountsOnlyVideos() throws Exception
    {
        existingVideo(2600);
        //2600 + 500 > 3000: video olmaz, ama resim (kapak) toplam kotaya sayılmaz
        ask("video/mp4", 500).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.limit").value("TOTAL"));
        ask("image/jpeg", 500).andExpect(status().isOk());
    }

    @Test
    void dailyQuotaCountsEveryRequestedUpload() throws Exception
    {
        ask("video/mp4", 1000).andExpect(status().isOk());
        ask("image/jpeg", 1000).andExpect(status().isOk());
        //Son 24 saatte 2000 + 600 > 2500 (dosya hiçbir videoya bağlanmasa da sayılır)
        ask("image/jpeg", 600).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.limit").value("DAILY"));
        ask("image/jpeg", 500).andExpect(status().isOk());
    }

    @Test
    void quotaEndpointShowsUsage() throws Exception
    {
        existingVideo(1200);
        ask("video/mp4", 300).andExpect(status().isOk());
        mockMvc.perform(get("/api/files/quota").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedBytes").value(1200))
                .andExpect(jsonPath("$.totalBytes").value(3000))
                .andExpect(jsonPath("$.dailyUsedBytes").value(300))
                .andExpect(jsonPath("$.dailyBytes").value(2500))
                .andExpect(jsonPath("$.maxFileBytes").value(1000));
        mockMvc.perform(get("/api/files/quota")).andExpect(status().isUnauthorized());
    }
}
