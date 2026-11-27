package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.enums.UserRole;
import com.yutkubeygo.watchit.repository.*;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Şikayet et + yönetim paneli: kim neye erişir, içerik kaldırma ve yoksayma ne yapar (gerçek PostgreSQL, sonunda geri alınır)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User owner;
    private User reporter;
    private User admin;
    private Video video;
    private Comment comment;

    @BeforeEach
    void setUp()
    {
        owner = saveUser("sikayet_testi_sahip", UserRole.USER);
        reporter = saveUser("sikayet_testi_bildiren", UserRole.USER);
        admin = saveUser("sikayet_testi_yonetici", UserRole.ADMIN);

        Channel channel = new Channel();
        channel.setChannelName("Şikayet testi kanalı");
        channel.setHandle("sikayet_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Şikayet edilecek video");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(10);
        videoRepository.saveAndFlush(video);

        comment = new Comment();
        comment.setContent("şikayet edilecek yorum");
        comment.setUser(owner);
        comment.setVideo(video);
        commentRepository.saveAndFlush(comment);
    }

    private User saveUser(String name, UserRole role)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String bearer(User user)
    {
        return "Bearer " + jwtService.generateToken(user);
    }

    private String body(String type, Long id)
    {
        return "{\"targetType\":\"" + type + "\",\"targetId\":" + id + ",\"reason\":\"SPAM\",\"details\":\"reklam\"}";
    }

    private void report(User user, String type, Long id, int expected) throws Exception
    {
        mockMvc.perform(post("/reports").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body(type, id)))
                .andExpect(status().is(expected));
    }

    @Test
    void reportingRules() throws Exception
    {
        mockMvc.perform(post("/reports").contentType(MediaType.APPLICATION_JSON).content(body("VIDEO", video.getId())))
                .andExpect(status().isUnauthorized());
        report(reporter, "VIDEO", video.getId(), 201);
        report(reporter, "VIDEO", video.getId(), 409);      //ikinci kez
        report(owner, "VIDEO", video.getId(), 400);         //kendi videosu
        report(reporter, "VIDEO", 99_999_999L, 404);        //olmayan video
        mockMvc.perform(post("/reports").header("Authorization", bearer(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"VIDEO\",\"targetId\":1,\"reason\":\"YOK\"}"))
                .andExpect(status().isBadRequest());        //bilinmeyen sebep
    }

    @Test
    void onlyAdminSeesThePanel() throws Exception
    {
        report(reporter, "VIDEO", video.getId(), 201);

        mockMvc.perform(get("/admin/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/reports").header("Authorization", bearer(reporter))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.targetId == " + video.getId() + ")]", hasSize(1)))
                .andExpect(jsonPath("$.items[?(@.targetId == " + video.getId() + ")].targetText").value("Şikayet edilecek video"))
                .andExpect(jsonPath("$.items[?(@.targetId == " + video.getId() + ")].targetOwnerUsername").value("sikayet_testi_sahip"));
        //Kategori yazma da yalnız yönetici: normal kullanıcı 403
        mockMvc.perform(post("/categories").header("Authorization", bearer(reporter))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Deneme\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeContentDeletesVideoNotifiesOwnerAndClosesReports() throws Exception
    {
        User second = saveUser("sikayet_testi_ikinci", UserRole.USER);
        report(reporter, "VIDEO", video.getId(), 201);
        report(second, "VIDEO", video.getId(), 201);
        Long reportId = reportRepository.findAll().stream()
                .filter(r -> r.getTargetId().equals(video.getId()) && r.getReporter().getId().equals(reporter.getId()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(post("/admin/reports/" + reportId + "/remove-content").header("Authorization", bearer(reporter)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/reports/" + reportId + "/remove-content").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(videoRepository.existsById(video.getId())).isFalse();
        //İki şikayet de kapandı
        assertThat(reportRepository.countByTargetTypeAndTargetIdAndStatus(
                com.yutkubeygo.watchit.enums.ReportTargetType.VIDEO, video.getId(),
                com.yutkubeygo.watchit.enums.ReportStatus.OPEN)).isZero();
        assertThat(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(owner.getId()))
                .anyMatch(n -> n.getType() == NotificationType.SYSTEM);
        //Kapanmış şikayete ikinci karar 409
        mockMvc.perform(post("/admin/reports/" + reportId + "/dismiss").header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void dismissKeepsTheComment() throws Exception
    {
        report(reporter, "COMMENT", comment.getId(), 201);
        Long reportId = reportRepository.findAll().stream()
                .filter(r -> r.getTargetId().equals(comment.getId())).findFirst().orElseThrow().getId();

        mockMvc.perform(post("/admin/reports/" + reportId + "/dismiss").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(commentRepository.existsById(comment.getId())).isTrue();
        mockMvc.perform(get("/admin/reports").param("status", "DISMISSED").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.items[?(@.id == " + reportId + ")].status").value("DISMISSED"));
    }
}
