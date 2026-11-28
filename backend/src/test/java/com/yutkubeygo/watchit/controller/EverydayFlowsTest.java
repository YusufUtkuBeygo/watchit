package com.yutkubeygo.watchit.controller;

import com.jayway.jsonpath.JsonPath;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Testi olmayan gündelik akışlar: kanal kuralları, izleme geçmişi (kaldığın yer), daha sonra izle.
//Gerçek PostgreSQL; her test sonunda geri alınır.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EverydayFlowsTest {

    private static final String FILE = "http://localhost:9000/watchit-images/uzun-videolar/tears_of_steel.mp4";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    private String token;
    private long videoId;

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, String body)
    {
        req.header("Authorization", "Bearer " + token);
        if(body != null)
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        return req;
    }

    private long id(MockHttpServletRequestBuilder req) throws Exception
    {
        String json = mvc.perform(req).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.parse(json).read("$.id")).longValue();
    }

    @BeforeEach
    void setUp() throws Exception
    {
        User u = new User();
        u.setUsername("gundelik_testi");
        u.setEmail("gundelik_testi@test.local");
        u.setPassword("x");
        u.setEmailVerified(true);
        token = jwtService.generateToken(userRepository.saveAndFlush(u));
        mvc.perform(as(post("/channels"), "{\"channelName\":\"Gündelik kanal\",\"handle\":\"x\"}"))
                .andExpect(status().isCreated());
        long categoryId = categoryRepository.findAll().getFirst().getId();
        videoId = id(as(post("/videos"), "{\"title\":\"Gündelik\",\"videoUrl\":\"" + FILE
                + "\",\"durationInSeconds\":600,\"categoryId\":" + categoryId + "}"));
    }

    @Test
    void oneChannelPerAccountAndHandleIgnoresComputerLanguage() throws Exception
    {
        //Kanal Türkçe dil ayarındayken açılır: kısa ad yine "isparta-..." olmalı
        //(eskiden parametresiz toLowerCase "ısparta" yapardı)
        User other = new User();
        other.setUsername("gundelik_dil");
        other.setEmail("gundelik_dil@test.local");
        other.setPassword("x");
        other.setEmailVerified(true);
        String otherToken = jwtService.generateToken(userRepository.saveAndFlush(other));
        Locale before = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            mvc.perform(post("/channels").header("Authorization", "Bearer " + otherToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"channelName\":\"ISPARTA Yolu\",\"handle\":\"x\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.handle").value("isparta-yolu"));
        }
        finally
        {
            Locale.setDefault(before);
        }
        //İkinci kanal açılamaz (her hesabın tek kanalı)
        mvc.perform(as(post("/channels"), "{\"channelName\":\"İkinci kanal\",\"handle\":\"y\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void watchHistoryKeepsOneRowPerVideoAndRemembersPosition() throws Exception
    {
        long first = id(as(post("/watchHistory"), "{\"videoId\":" + videoId + ",\"watchedSeconds\":30}"));
        long second = id(as(post("/watchHistory"), "{\"videoId\":" + videoId + ",\"watchedSeconds\":95,\"isCompleted\":false}"));
        //Aynı video için ikinci kayıt açılmaz, var olan güncellenir (kaldığın yerden devam)
        org.junit.jupiter.api.Assertions.assertEquals(first, second);
        mvc.perform(as(get("/watchHistory").param("videoId", String.valueOf(videoId)), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].watchedSeconds").value(95));
        //Olmayan video 404, eksi saniye 400
        mvc.perform(as(post("/watchHistory"), "{\"videoId\":99999999,\"watchedSeconds\":1}")).andExpect(status().isNotFound());
        mvc.perform(as(post("/watchHistory"), "{\"videoId\":" + videoId + ",\"watchedSeconds\":-5}")).andExpect(status().isBadRequest());
    }

    @Test
    void watchLaterAddTwiceIsHarmlessAndRemoveWorksOnce() throws Exception
    {
        String body = "{\"videoId\":" + videoId + "}";
        mvc.perform(as(post("/watchLater"), body)).andExpect(status().isCreated());
        //İki kez basılırsa hata değil
        mvc.perform(as(post("/watchLater"), body)).andExpect(status().isOk());
        mvc.perform(as(get("/watchLater"), null))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(videoId));
        mvc.perform(as(delete("/watchLater").param("videoId", String.valueOf(videoId)), null)).andExpect(status().isNoContent());
        mvc.perform(as(delete("/watchLater").param("videoId", String.valueOf(videoId)), null)).andExpect(status().isNotFound());
        mvc.perform(as(post("/watchLater"), "{\"videoId\":99999999}")).andExpect(status().isNotFound());
    }
}
