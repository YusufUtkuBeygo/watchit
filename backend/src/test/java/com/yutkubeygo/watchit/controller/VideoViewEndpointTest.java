package com.yutkubeygo.watchit.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Görüntülenme ucunun güvenlik kuralı: yalnız POST /videos/{id}/view girişsiz açık, başka hiçbir şey açılmadı.
//Uygulamanın tamamı (veritabanı dahil) açılır; testler veri yazmaz.
@SpringBootTest
@AutoConfigureMockMvc
class VideoViewEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousViewOfMissingVideoIs404NotUnauthorized() throws Exception
    {
        //401 değil 404: kural girişsiz isteği içeri aldı, video olmadığı için servis 404 seçti
        mockMvc.perform(post("/videos/999999999/view")).andExpect(status().isNotFound());
    }

    @Test
    void otherMethodsOnViewAddressStillNeedLogin() throws Exception
    {
        mockMvc.perform(put("/videos/1/view")).andExpect(status().isUnauthorized());
    }

    @Test
    void creatingVideoStillNeedsLogin() throws Exception
    {
        mockMvc.perform(post("/videos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
