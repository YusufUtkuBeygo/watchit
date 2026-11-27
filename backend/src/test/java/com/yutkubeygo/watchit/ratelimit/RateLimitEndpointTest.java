package com.yutkubeygo.watchit.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Sınır gerçekten istek yolunda mı: aynı IP'den 6. kayıt denemesi 429 + Retry-After.
//Kayıtlar bilerek geçersiz (boş gövde, 400): sayaç doğrulamadan önce sayar, veritabanına kullanıcı yazılmaz.
@SpringBootTest(properties = "rate-limit.enabled=true")
@AutoConfigureMockMvc
class RateLimitEndpointTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void sixthRegistrationFromSameIpIsRejected() throws Exception
    {
        for(int i = 0; i < 5; i++)
            mockMvc.perform(post("/users").with(r -> { r.setRemoteAddr("10.9.8.7"); return r; })
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

        mockMvc.perform(post("/users").with(r -> { r.setRemoteAddr("10.9.8.7"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"));

        //Başka IP etkilenmez, okuma (GET) hiç sayılmaz
        mockMvc.perform(post("/users").with(r -> { r.setRemoteAddr("10.9.8.8"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories").with(r -> { r.setRemoteAddr("10.9.8.7"); return r; }))
                .andExpect(status().isOk());
    }
}
