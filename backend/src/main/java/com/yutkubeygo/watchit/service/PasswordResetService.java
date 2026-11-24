package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.EmailToken;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.EmailTokenPurpose;
import com.yutkubeygo.watchit.repository.EmailTokenRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

//"Şifremi unuttum": e-postaya tek kullanımlık bağlantı gönderir, bağlantıyla yeni şifre belirlenir.
@Service
public class PasswordResetService {

    static final Duration LINK_LIFETIME = Duration.ofMinutes(30);  //bağlantı 30 dakika geçerli
    static final int MAX_REQUESTS = 3;                              //aynı hesaba 15 dakikada en fazla 3 mail
    static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final EmailTokenRepository tokenRepository;
    private final TokenHasher hasher;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final String frontendUrl;

    public PasswordResetService(UserRepository userRepository, EmailTokenRepository tokenRepository, TokenHasher hasher,
                                EmailService emailService, PasswordEncoder passwordEncoder,
                                LoginAttemptService loginAttemptService, @Value("${app.frontend-url}") String frontendUrl)
    {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.hasher = hasher;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.frontendUrl = frontendUrl;
    }

    //Hiçbir şey döndürmez: e-posta kayıtlı olsun olmasın controller aynı cevabı verir (202).
    //Yoksa bu ekran "bu e-posta sitede kayıtlı mı?" sorusunu cevaplayan bir araca dönüşürdü (kullanıcı numaralandırma).
    @Transactional
    public void requestReset(String rawEmail)
    {
        User user = userRepository.findByEmail(UserService.normalizeEmail(rawEmail)).orElse(null);
        if(user == null)
            return;

        LocalDateTime now = LocalDateTime.now();
        //Biri başkasının adresine durmadan mail yağdıramasın: sınır aşılınca sessizce gönderilmez
        if(tokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), EmailTokenPurpose.RESET_PASSWORD, now.minus(REQUEST_WINDOW)) >= MAX_REQUESTS)
            return;

        //Önceki bağlantılar geçersiz: yalnız en son gönderilen çalışır
        tokenRepository.invalidateAll(user.getId(), EmailTokenPurpose.RESET_PASSWORD, now);

        String token = hasher.newUrlToken();
        EmailToken row = new EmailToken();
        row.setUser(user);
        row.setPurpose(EmailTokenPurpose.RESET_PASSWORD);
        row.setTokenHash(hasher.hashUrlToken(token));
        row.setExpiresAt(now.plus(LINK_LIFETIME));
        tokenRepository.save(row);

        String link = frontendUrl + "/sifre-sifirla?token=" + token;
        emailService.send(user.getEmail(), "WatchIt şifreni sıfırla",
                "Merhaba " + user.getUsername() + ",\n\nŞifreni sıfırlamak için bu bağlantıyı aç (30 dakika geçerli, tek kullanımlık):\n"
                        + link + "\n\nBu isteği sen yapmadıysan bu maili yok sayabilirsin; şifren değişmez.",
                "<p>Merhaba " + EmailVerificationService.escape(user.getUsername()) + ",</p>"
                        + "<p>Şifreni sıfırlamak için aşağıdaki düğmeye bas. Bağlantı 30 dakika geçerli ve tek kullanımlık.</p>"
                        + "<p><a href=\"" + link + "\" style=\"display:inline-block;padding:12px 20px;border-radius:999px;"
                        + "background:#f2c230;color:#121212;font-weight:700;text-decoration:none\">Şifremi sıfırla</a></p>"
                        + "<p>Bu isteği sen yapmadıysan bu maili yok sayabilirsin; şifren değişmez.</p>");
    }

    //Bağlantıdaki token geçerliyse şifreyi değiştirir. Geçersiz, süresi dolmuş ya da kullanılmışsa false (400).
    @Transactional
    public boolean resetPassword(String token, String newPassword)
    {
        LocalDateTime now = LocalDateTime.now();
        //Veritabanında token'ın kendisi yok, hash'i var: gelen token'ı da hash'leyip eşleşeni ararız
        EmailToken row = tokenRepository.findByTokenHashAndPurpose(hasher.hashUrlToken(token), EmailTokenPurpose.RESET_PASSWORD)
                .stream().filter(t -> t.isActive(now)).findFirst().orElse(null);
        if(row == null)
            return false;

        User user = row.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        //Bundan önce verilmiş bütün giriş anahtarları geçersiz (JwtAuthFilter): çalınmış bir oturum da kapanır
        user.setPasswordChangedAt(now);
        //Maildeki bağlantıyı açabildiyse o adresin sahibi: e-posta da doğrulanmış sayılır
        user.setEmailVerified(true);
        userRepository.save(user);

        //Bağlantı tek kullanımlık: bu kayıt nesne üzerinden de işaretlenir (toplu UPDATE, Hibernate'in hafızasındaki
        //nesneyi güncellemez), varsa diğer bağlantılar da iptal
        row.setUsedAt(now);
        tokenRepository.save(row);
        tokenRepository.invalidateAll(user.getId(), EmailTokenPurpose.RESET_PASSWORD, now);
        //Yanlış şifre denemeleri yüzünden kilitlendiyse kilit kalkar
        loginAttemptService.recordSuccess(user.getEmail());
        return true;
    }
}
