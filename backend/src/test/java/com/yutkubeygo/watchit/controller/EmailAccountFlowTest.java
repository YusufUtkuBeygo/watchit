package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.EmailToken;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.EmailTokenPurpose;
import com.yutkubeygo.watchit.repository.EmailTokenRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.EmailService;
import com.yutkubeygo.watchit.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//E-posta doğrulama, şifremi unuttum ve eski oturumların kapanması, gerçek PostgreSQL'e karşı uçtan uca.
//Mail gerçekten gönderilmez: EmailService sahte (@MockitoBean); gönderilen metin yakalanıp içindeki kod/bağlantı okunur.
//@Transactional: test sonunda eklenen her şey geri alınır.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailAccountFlowTest {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");
    private static final Pattern RESET_TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailTokenRepository tokenRepository;
    @Autowired private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    //Kayıt ucu üzerinden hesap açar (gerçek akış: kod maili tetiklenir)
    private User register(String name) throws Exception
    {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"email\":\"" + name + "@test.local\",\"password\":\"eski-sifre-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));
        return userRepository.findByEmail(name + "@test.local").orElseThrow();
    }

    //Sahte mail servisine giden son mailin düz metnini döndürür
    private String lastMailText(String to)
    {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce()).send(eq(to), anyString(), text.capture(), anyString());
        return text.getValue();
    }

    private static String find(Pattern pattern, String text)
    {
        Matcher m = pattern.matcher(text);
        assertThat(m.find()).as("mailde beklenen değer yok: " + text).isTrue();
        return m.group(1);
    }

    private String bearer(User user)
    {
        return "Bearer " + jwtService.generateToken(user);
    }

    private String verifyBody(String code)
    {
        return "{\"code\":\"" + code + "\"}";
    }

    @Test
    void registrationSendsCodeAndCorrectCodeVerifies() throws Exception
    {
        User user = register("dogrulama1");
        String code = find(CODE, lastMailText(user.getEmail()));

        //Kod veritabanında açık durmuyor, yalnız hash'i var
        EmailToken saved = tokenRepository.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                user.getId(), EmailTokenPurpose.VERIFY_EMAIL).orElseThrow();
        assertThat(saved.getTokenHash()).hasSize(64).doesNotContain(code);

        String wrong = code.equals("000000") ? "111111" : "000000";
        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(wrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("WRONG_CODE"));

        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(code)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
        //Aynı kod ikinci kez sorun çıkarmaz (zaten doğrulanmış)
        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(code)))
                .andExpect(status().isNoContent());
    }

    @Test
    void fiveWrongAttemptsKillTheCode() throws Exception
    {
        User user = register("dogrulama2");
        String code = find(CODE, lastMailText(user.getEmail()));
        String wrong = code.equals("000000") ? "111111" : "000000";

        for(int i = 0; i < 4; i++)
            mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                            .contentType(MediaType.APPLICATION_JSON).content(verifyBody(wrong)))
                    .andExpect(status().isBadRequest());
        //5. yanlış: kod ölür
        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(wrong)))
                .andExpect(status().isTooManyRequests());
        //Artık doğru kod da çalışmaz, yeni kod istenmeli
        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(code)))
                .andExpect(status().isTooManyRequests());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() throws Exception
    {
        User user = register("dogrulama3");
        String code = find(CODE, lastMailText(user.getEmail()));
        EmailToken token = tokenRepository.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                user.getId(), EmailTokenPurpose.VERIFY_EMAIL).orElseThrow();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.saveAndFlush(token);

        mockMvc.perform(post("/auth/verify-email").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(verifyBody(code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NO_ACTIVE_CODE"));
    }

    @Test
    void resendRightAfterRegistrationIsTooSoon() throws Exception
    {
        User user = register("dogrulama4");
        mockMvc.perform(post("/auth/verify-email/resend").header("Authorization", bearer(user)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unverifiedUserCannotCreateChannelOrComment() throws Exception
    {
        User user = register("dogrulama5");
        mockMvc.perform(post("/channels").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"channelName\":\"Doğrulanmamış kanal\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_VERIFIED"));
        mockMvc.perform(post("/comments").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"merhaba\",\"videoId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/files/upload-url").header("Authorization", bearer(user))
                        .param("fileName", "a.mp4").param("contentType", "video/mp4").param("fileSize", "1000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgotPasswordAnswersTheSameForUnknownEmail() throws Exception
    {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kimse-yok@test.local\"}"))
                .andExpect(status().isAccepted());
        verify(emailService, never()).send(eq("kimse-yok@test.local"), anyString(), anyString(), anyString());
    }

    @Test
    void resetPasswordChangesPasswordAndClosesOldSessions() throws Exception
    {
        User user = register("sifirlama1");
        String oldToken = bearer(user);
        clearInvocations(emailService);

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"SIFIRLAMA1@test.local\"}"))
                .andExpect(status().isAccepted());
        String resetToken = find(RESET_TOKEN, lastMailText(user.getEmail()));

        //JWT'deki zaman saniye hassasiyetinde: eski token'ın "önce" verilmiş sayılması için bir saniye beklenir
        Thread.sleep(1100);
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"yeni-sifre-456\"}"))
                .andExpect(status().isNoContent());

        //Eski oturum kapandı
        mockMvc.perform(get("/users/me").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        //Eski şifre çalışmıyor, yenisi çalışıyor
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sifirlama1@test.local\",\"password\":\"eski-sifre-123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sifirlama1@test.local\",\"password\":\"yeni-sifre-456\"}"))
                .andExpect(status().isOk());
        //Bağlantı tek kullanımlık
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"baska-sifre-789\"}"))
                .andExpect(status().isBadRequest());
        //Maildeki bağlantıyı açabildiyse e-posta da doğrulanmış sayılır
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void madeUpResetTokenIsRejected() throws Exception
    {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"uydurma-token\",\"newPassword\":\"yeni-sifre-456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordChangeGivesNewTokenAndClosesOldOne() throws Exception
    {
        User user = register("sifredegis1");
        String oldToken = bearer(user);
        Thread.sleep(1100);

        String body = mockMvc.perform(put("/users/me/password").header("Authorization", oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"eski-sifre-123\",\"newPassword\":\"yeni-sifre-456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = "Bearer " + body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/users/me").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").header("Authorization", newToken)).andExpect(status().isOk());
    }

    @Test
    void deletedAccountTokenStopsWorking() throws Exception
    {
        User user = register("silinen1");
        String token = bearer(user);
        mockMvc.perform(delete("/users/me").header("Authorization", token)).andExpect(status().isNoContent());
        mockMvc.perform(get("/users/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void emailChangeNeedsNewVerification() throws Exception
    {
        User user = register("epostadegis1");
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
        clearInvocations(emailService);

        mockMvc.perform(put("/users/me").header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"epostadegis1\",\"email\":\"yeni-adres@test.local\",\"currentPassword\":\"eski-sifre-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false));
        //Kod yeni adrese gider
        find(CODE, lastMailText("yeni-adres@test.local"));
    }
}
