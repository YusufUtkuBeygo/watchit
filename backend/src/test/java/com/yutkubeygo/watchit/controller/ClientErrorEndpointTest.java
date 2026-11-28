package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.UserRole;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Hata takibi: tarayıcı hatayı girişsiz de bildirebilir, yalnız yönetici görür, uzun metin kesilir
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientErrorEndpointTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    private String token(String name, UserRole role)
    {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.local");
        u.setPassword("x");
        u.setRole(role);
        return "Bearer " + jwtService.generateToken(userRepository.saveAndFlush(u));
    }

    @Test
    void anyoneCanReportOnlyAdminCanRead() throws Exception
    {
        String longMessage = "hata-testi-" + "x".repeat(900);
        mvc.perform(post("/client-errors").header("User-Agent", "Deneme/1.0").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longMessage + "\",\"stack\":\"at Bilesen\",\"pageUrl\":\"/watch/1\"}"))
                .andExpect(status().isNoContent());
        //Boş mesaj kabul edilmez
        mvc.perform(post("/client-errors").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());

        String admin = token("hata_testi_yonetici", UserRole.ADMIN);
        mvc.perform(get("/admin/client-errors").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].message", hasLength(500)))
                .andExpect(jsonPath("$.items[0].pageUrl").value("/watch/1"))
                .andExpect(jsonPath("$.items[0].userAgent").value("Deneme/1.0"));
        mvc.perform(get("/admin/summary").header("Authorization", admin))
                .andExpect(jsonPath("$.clientErrors24h", greaterThanOrEqualTo(1)));

        mvc.perform(get("/admin/client-errors").header("Authorization", token("hata_testi_kullanici", UserRole.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/client-errors")).andExpect(status().isUnauthorized());
    }
}
