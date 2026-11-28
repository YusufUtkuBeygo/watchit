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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Sahiplik ve yetki (güvenlik testi 27 Kasım'daki elle yapılan IDOR denemelerinin kalıcı hâli).
//A (kurban) her türden kayıt oluşturur; B (saldırgan) aynı token'la A'nın kayıtlarını okumaya, değiştirmeye, silmeye çalışır.
//Biri ileride bir servisteki sahiplik kontrolünü yanlışlıkla silerse bu test kırılır.
//Gerçek PostgreSQL; @Transactional: her test sonunda her şey geri alınır.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnershipAccessTest {

    private static final String FILE = "http://localhost:9000/watchit-images/uzun-videolar/tears_of_steel.mp4";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    private String a;   //kurbanın token'ı
    private String b;   //saldırganın token'ı
    private long categoryId;

    //A'nın kayıtları
    private long channelA, videoA, commentA, playlistA, playlistVideoA, clipA, historyA, videoLikeA, commentLikeA, subscriptionA, notificationA;
    private long channelB, videoB;

    private User user(String name)
    {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.local");
        u.setPassword("x");
        u.setEmailVerified(true);
        return userRepository.saveAndFlush(u);
    }

    //İstek atar, beklenen kodu kontrol eder, cevaptaki id'yi döner (yoksa -1)
    private long call(MockHttpServletRequestBuilder req, String token, String body, ResultMatcher expected) throws Exception
    {
        if(token != null)
            req.header("Authorization", "Bearer " + token);
        if(body != null)
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        String json = mvc.perform(req).andExpect(expected).andReturn().getResponse().getContentAsString();
        if(json.isBlank() || !json.trim().startsWith("{"))
            return -1;
        Object id = JsonPath.parse(json).read("$.id");
        return id == null ? -1 : ((Number) id).longValue();
    }

    private long create(String url, String token, String body) throws Exception
    {
        return call(post(url), token, body, status().is2xxSuccessful());
    }

    @BeforeEach
    void setUp() throws Exception
    {
        User userA = user("yetki_kurban");
        User userB = user("yetki_saldirgan");
        a = jwtService.generateToken(userA);
        b = jwtService.generateToken(userB);
        categoryId = categoryRepository.findAll().getFirst().getId();

        channelA = create("/channels", a, "{\"channelName\":\"Kurban kanalı\",\"handle\":\"yetki_kurban\"}");
        channelB = create("/channels", b, "{\"channelName\":\"Saldırgan kanalı\",\"handle\":\"yetki_saldirgan\"}");
        videoA = create("/videos", a, video("Kurban videosu"));
        videoB = create("/videos", b, video("Saldırgan videosu"));
        commentA = create("/comments", a, "{\"content\":\"kurban yorumu\",\"videoId\":" + videoA + "}");
        long commentAOnB = create("/comments", a, "{\"content\":\"A, B'nin videosunda\",\"videoId\":" + videoB + "}");
        playlistA = create("/playlists", a, "{\"title\":\"Gizli liste\",\"isPublic\":false}");
        playlistVideoA = create("/playlistvideos", a, "{\"playlistId\":" + playlistA + ",\"videoId\":" + videoA + ",\"orderIndex\":0}");
        clipA = create("/clips", a, "{\"videoId\":" + videoA + ",\"startSeconds\":10,\"endSeconds\":30,\"title\":\"klip\"}");
        historyA = create("/watchHistory", a, "{\"videoId\":" + videoA + ",\"watchedSeconds\":5}");
        videoLikeA = create("/videoLikes", a, "{\"videoId\":" + videoB + "}");
        commentLikeA = create("/commentLikes", a, "{\"commentId\":" + commentAOnB + "}");
        subscriptionA = create("/subscriptions", a, "{\"channelId\":" + channelB + "}");
        //B, A'ya abone olur: A'ya bildirim düşer
        create("/subscriptions", b, "{\"channelId\":" + channelA + "}");
        String notifications = mvc.perform(get("/notifications").header("Authorization", "Bearer " + a))
                .andReturn().getResponse().getContentAsString();
        notificationA = ((Number) JsonPath.parse(notifications).read("$.items[0].id")).longValue();
    }

    private String video(String title)
    {
        return "{\"title\":\"" + title + "\",\"videoUrl\":\"" + FILE + "\",\"durationInSeconds\":600,\"categoryId\":" + categoryId + "}";
    }

    @Test
    void attackerCannotChangeOrDeleteVictimsContent() throws Exception
    {
        call(put("/videos/" + videoA), b, video("ele geçirildi"), status().isForbidden());
        call(delete("/videos/" + videoA), b, null, status().isForbidden());
        call(post("/videos/" + videoA + "/transcript"), b, null, status().isForbidden());
        call(put("/channels/" + channelA), b, "{\"channelName\":\"ele\",\"handle\":\"ele\"}", status().isForbidden());
        call(delete("/channels/" + channelA), b, null, status().isForbidden());
        call(put("/comments/" + commentA), b, "{\"content\":\"ele\",\"videoId\":" + videoA + "}", status().isForbidden());
        call(delete("/comments/" + commentA), b, null, status().isForbidden());
        call(put("/comments/" + commentA + "/pin"), b, null, status().isForbidden());
        call(delete("/comments/pin?videoId=" + videoA), b, null, status().isForbidden());
        call(delete("/clips/" + clipA), b, null, status().isForbidden());
    }

    @Test
    void attackerCannotReadOrChangePrivatePlaylist() throws Exception
    {
        call(get("/playlists/" + playlistA), b, null, status().isForbidden());
        call(get("/playlists/" + playlistA), null, null, status().isForbidden());
        call(get("/playlistvideos?playlistId=" + playlistA), b, null, status().isForbidden());
        call(get("/playlistvideos/" + playlistVideoA), b, null, status().isForbidden());
        call(put("/playlists/" + playlistA), b, "{\"title\":\"ele\",\"isPublic\":true}", status().isForbidden());
        call(delete("/playlists/" + playlistA), b, null, status().isForbidden());
        call(post("/playlistvideos"), b, "{\"playlistId\":" + playlistA + ",\"videoId\":" + videoB + ",\"orderIndex\":1}", status().isForbidden());
        call(put("/playlistvideos/" + playlistVideoA), b, "{\"playlistId\":" + playlistA + ",\"videoId\":" + videoA + ",\"orderIndex\":5}", status().isForbidden());
        call(delete("/playlistvideos/" + playlistVideoA), b, null, status().isForbidden());
    }

    @Test
    void attackerCannotSeeVictimsPersonalRecords() throws Exception
    {
        call(get("/watchHistory/" + historyA), b, null, status().isForbidden());
        call(delete("/watchHistory/" + historyA), b, null, status().isForbidden());
        call(get("/notifications/" + notificationA), b, null, status().isForbidden());
        call(put("/notifications/" + notificationA + "/read"), b, null, status().isForbidden());
        call(delete("/notifications/" + notificationA), b, null, status().isForbidden());
        call(get("/videoLikes/" + videoLikeA), b, null, status().isForbidden());
        call(get("/commentLikes/" + commentLikeA), b, null, status().isForbidden());
        call(delete("/commentLikes/" + commentLikeA), b, null, status().isForbidden());
        call(get("/subscriptions/" + subscriptionA), b, null, status().isForbidden());
        //Herkese açık kullanıcı bilgisinde e-posta ve rol yok
        mvc.perform(get("/users/" + userRepository.findByEmail("yetki_kurban@test.local").orElseThrow().getId())
                        .header("Authorization", "Bearer " + b))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("yetki_kurban@test.local"))))
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void normalUserCannotUseAdminFeatures() throws Exception
    {
        for(String path : new String[]{"/admin/summary", "/admin/reports", "/admin/feedback"})
        {
            call(get(path), b, null, status().isForbidden());
            call(get(path), null, null, status().isUnauthorized());
        }
        call(post("/admin/reports/1/dismiss"), b, null, status().isForbidden());
        call(post("/admin/reports/1/remove-content"), b, null, status().isForbidden());
        call(post("/categories"), b, "{\"name\":\"hack\"}", status().isForbidden());
        call(delete("/categories/" + categoryId), b, null, status().isForbidden());
    }

    @Test
    void hiddenFieldsInRequestsAreIgnored() throws Exception
    {
        //Videoyu başkasının kanalına yazmak ve sayacı şişirmek
        long sneaky = create("/videos", b, "{\"title\":\"x\",\"videoUrl\":\"" + FILE + "\",\"durationInSeconds\":1,\"categoryId\":"
                + categoryId + ",\"channelId\":" + channelA + ",\"viewCount\":999999}");
        mvc.perform(get("/videos/" + sneaky))
                .andExpect(jsonPath("$.channelId").value(channelB))
                .andExpect(jsonPath("$.viewCount").value(0));
        //Profilde rol yükseltme
        call(put("/users/me"), b, "{\"username\":\"yetki_saldirgan\",\"email\":\"yetki_saldirgan@test.local\",\"role\":\"ADMIN\"}",
                status().isOk());
        call(get("/admin/summary"), b, null, status().isForbidden());
    }
}
