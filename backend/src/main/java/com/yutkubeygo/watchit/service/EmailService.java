package com.yutkubeygo.watchit.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

//E-posta gönderir. JavaMailSender'ı Spring Boot kurar (spring.mail.* ayarları): SMTP sunucusuna bağlanıp mesajı teslim eder.
//Geliştirmede sunucu Mailpit (localhost:1025), yayında gerçek bir servis; bu sınıf ikisinde de aynı.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from)
    {
        this.mailSender = mailSender;
        this.from = from;
    }

    //Arka planda gönderir (@Async, ayrı iş parçacığı). İki sebep:
    //1) İstek posta sunucusunu beklemesin (gerçek servislerde 1-2 sn sürebilir).
    //2) "Şifremi unuttum"da cevap süresi e-postanın kayıtlı olup olmadığını ele vermesin: kayıtlıysa mail
    //   gönderilir ama kullanıcı bunu beklemez, iki durumda da cevap aynı hızda döner.
    //Gönderilemezse hata kullanıcıya gitmez, günlüğe yazılır; kullanıcı "kodu tekrar gönder" diyebilir.
    @Async("mailExecutor")
    public void send(String to, String subject, String text, String html)
    {
        try
        {
            MimeMessage message = mailSender.createMimeMessage();
            //true: hem düz metin hem HTML (mail programı hangisini gösterebiliyorsa onu gösterir); UTF-8: Türkçe harfler
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
            log.info("Mail gönderildi: {} ({})", subject, to);
        }
        catch(MailException | MessagingException e)
        {
            log.warn("Mail gönderilemedi ({}): {}", to, e.getMessage());
        }
    }
}
