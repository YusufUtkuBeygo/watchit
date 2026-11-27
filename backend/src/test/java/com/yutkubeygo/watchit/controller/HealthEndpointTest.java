package com.yutkubeygo.watchit.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Yayın paketi: sağlık ucu girişsiz açık ve ayrıntı sızdırmıyor; diğer actuator uçları kapalı
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthIsPublicAndTerse() throws Exception
    {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        //Ortam değişkenlerini gösteren uç açık değil (açık olsaydı şifreler görünebilirdi)
        mockMvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }
}
