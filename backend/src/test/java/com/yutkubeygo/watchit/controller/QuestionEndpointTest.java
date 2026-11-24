package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.JwtService;
import com.yutkubeygo.watchit.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//POST /videos/{id}/questions: giriş ister, boş ya da 500 karakterden uzun soru 400 (servise hiç gidilmez).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    //Uygulamanın kendi JwtService'iyle üretilmiş geçerli token. Kullanıcı gerçekten kaydedilir:
    //JwtAuthFilter artık var olmayan (silinmiş) hesabın token'ını kabul etmiyor. @Transactional ile geri alınır.
    private String bearer()
    {
        User user = new User();
        user.setUsername("soru_testi");
        user.setEmail("soru_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void withoutLoginIs401() throws Exception
    {
        mockMvc.perform(post("/videos/1/questions").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Soru?\"}"))
                .andExpect(status().isUnauthorized());
        verify(questionService, never()).ask(anyLong(), anyLong(), anyString());
    }

    @Test
    void blankQuestionIs400() throws Exception
    {
        mockMvc.perform(post("/videos/1/questions").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tooLongQuestionIs400() throws Exception
    {
        mockMvc.perform(post("/videos/1/questions").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verify(questionService, never()).ask(anyLong(), anyLong(), anyString());
    }
}
