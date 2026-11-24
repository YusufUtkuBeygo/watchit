package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.EmailToken;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.EmailTokenPurpose;
import com.yutkubeygo.watchit.exception.EmailNotVerifiedException;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.repository.EmailTokenRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

//E-posta doğrulama: kayıtta (ve e-posta değişince) 6 haneli kod gönderir, kod girilince hesabı doğrulanmış yapar.
@Service
public class EmailVerificationService {

    static final Duration CODE_LIFETIME = Duration.ofMinutes(15);  //kod 15 dakika geçerli
    static final int MAX_ATTEMPTS = 5;                              //bir koda en fazla 5 yanlış deneme
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60); //"tekrar gönder" en sık dakikada bir
    static final int MAX_CODES_PER_HOUR = 5;                        //saatte en fazla 5 kod (mail kutusunu boğmasın)

    public enum Result { VERIFIED, ALREADY_VERIFIED, WRONG_CODE, NO_ACTIVE_CODE, TOO_MANY_ATTEMPTS }

    private final EmailTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher hasher;
    private final EmailService emailService;

    public EmailVerificationService(EmailTokenRepository tokenRepository, UserRepository userRepository,
                                    TokenHasher hasher, EmailService emailService)
    {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.emailService = emailService;
    }

    //"Kodu tekrar gönder" düğmesi: son koddan 60 saniye geçmeden yenisi gönderilmez (429).
    @Transactional
    public void resendCode(User user)
    {
        EmailToken last = tokenRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId(), EmailTokenPurpose.VERIFY_EMAIL)
                .orElse(null);
        if(last != null && last.getCreatedAt() != null && last.getCreatedAt().isAfter(LocalDateTime.now().minus(RESEND_COOLDOWN)))
            throw new TooManyRequestsException();
        sendCode(user);
    }

    //Yeni kod üretir, eskileri iptal eder, mail atar. Saatte 5'ten fazlaysa 429.
    //(Kayıt ve e-posta değişikliği bu yoldan gelir: yeni adrese kod 60 saniye beklemeden gitmeli.)
    //Kod veritabanına hash olarak yazılır; açık hâli yalnız maile girer ve hiçbir yerde saklanmaz.
    @Transactional
    public void sendCode(User user)
    {
        LocalDateTime now = LocalDateTime.now();
        if(tokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), EmailTokenPurpose.VERIFY_EMAIL, now.minusHours(1)) >= MAX_CODES_PER_HOUR)
            throw new TooManyRequestsException();

        //Aynı anda tek geçerli kod: yenisi gelince eskiler çalışmaz
        tokenRepository.invalidateAll(user.getId(), EmailTokenPurpose.VERIFY_EMAIL, now);

        String code = hasher.newSixDigitCode();
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setPurpose(EmailTokenPurpose.VERIFY_EMAIL);
        token.setTokenHash(hasher.hashCode(user.getId(), code));
        token.setExpiresAt(now.plus(CODE_LIFETIME));
        tokenRepository.save(token);

        emailService.send(user.getEmail(), "WatchIt doğrulama kodun: " + code,
                "Merhaba " + user.getUsername() + ",\n\nWatchIt doğrulama kodun: " + code
                        + "\n\nKod 15 dakika geçerli. Bu isteği sen yapmadıysan bu maili yok sayabilirsin.",
                "<p>Merhaba " + escape(user.getUsername()) + ",</p>"
                        + "<p>WatchIt doğrulama kodun:</p>"
                        + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px\">" + code + "</p>"
                        + "<p>Kod 15 dakika geçerli. Bu isteği sen yapmadıysan bu maili yok sayabilirsin.</p>");
    }

    //Kayıt sırasında: mail gönderilemese bile kayıt bozulmasın (kullanıcı "tekrar gönder" diyebilir)
    public void sendCodeQuietly(User user)
    {
        try
        {
            sendCode(user);
        }
        catch(RuntimeException ignored)
        {
            //sınıra takıldıysa ya da veritabanı hatasıysa kayıt yine başarılı; doğrulama sonra yapılır
        }
    }

    //Kullanıcının girdiği kodu kontrol eder.
    @Transactional
    public Result verify(Long userId, String code)
    {
        User user = userRepository.findById(userId).orElseThrow();
        if(user.isEmailVerified())
            return Result.ALREADY_VERIFIED;

        EmailToken token = tokenRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(userId, EmailTokenPurpose.VERIFY_EMAIL)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if(token == null || !token.isActive(now))
            return Result.NO_ACTIVE_CODE;

        //5 yanlıştan sonra bu kod ölür: 1 milyon olasılıkta 5 deneme = şansla bulma ihtimali yüz binde beş
        if(token.getAttempts() >= MAX_ATTEMPTS)
            return Result.TOO_MANY_ATTEMPTS;

        if(!TokenHasher.sameHash(token.getTokenHash(), hasher.hashCode(userId, code)))
        {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            return token.getAttempts() >= MAX_ATTEMPTS ? Result.TOO_MANY_ATTEMPTS : Result.WRONG_CODE;
        }

        token.setUsedAt(now);
        tokenRepository.save(token);
        user.setEmailVerified(true);
        userRepository.save(user);
        return Result.VERIFIED;
    }

    //Video yükleme, yorum yazma gibi işlerin başında çağrılır: doğrulanmamışsa 403 EMAIL_NOT_VERIFIED
    public void requireVerified(Long userId)
    {
        User user = userRepository.findById(userId).orElse(null);
        if(user != null && !user.isEmailVerified())
            throw new EmailNotVerifiedException();
    }

    //Kullanıcı adı HTML'e girerken <, > gibi karakterler etiket sayılmasın (mailde HTML enjeksiyonu olmasın)
    static String escape(String text)
    {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
