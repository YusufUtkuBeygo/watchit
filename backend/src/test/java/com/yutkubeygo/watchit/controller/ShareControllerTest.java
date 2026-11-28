package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Paylaşım önizlemesi (link botları): başlık ve kapak etiketlerde, kullanıcı yazısı kaçırılmış, girişsiz açık
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShareControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void previewHasTagsAndEscapesUserText() throws Exception
    {
        User owner = new User();
        owner.setUsername("onizleme_testi");
        owner.setEmail("onizleme_testi@test.local");
        owner.setPassword("x");
        userRepository.saveAndFlush(owner);
        Channel channel = new Channel();
        channel.setChannelName("Önizleme kanalı");
        channel.setHandle("onizleme_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);
        Video video = new Video();
        video.setTitle("Çok güzel <script>alert(1)</script> \"video\"");
        video.setDescription("Açıklama");
        video.setThumbnailUrl("http://localhost:9000/watchit-images/kapak.jpg");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(10);
        videoRepository.saveAndFlush(video);

        //Girişsiz (botların token'ı yok)
        mvc.perform(get("/og/watch/" + video.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("property=\"og:title\" content=\"Çok güzel &lt;script&gt;alert(1)&lt;/script&gt; &quot;video&quot;\"")))
                .andExpect(content().string(containsString("property=\"og:image\" content=\"http://localhost:9000/watchit-images/kapak.jpg\"")))
                .andExpect(content().string(containsString("summary_large_image")))
                .andExpect(content().string(not(containsString("<script>"))));

        mvc.perform(get("/og/watch/99999999")).andExpect(status().isNotFound());
    }
}
