package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ForgotPasswordRequestDto;
import com.yutkubeygo.watchit.dto.ResetPasswordRequestDto;
import com.yutkubeygo.watchit.dto.VerifyEmailRequestDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.EmailVerificationService;
import com.yutkubeygo.watchit.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//Hesap güvenliği uçları: e-posta doğrulama ve şifremi unuttum.
//verify-email ve resend giriş ister; forgot/reset girişsiz açık (SecurityConfig), çünkü şifresini unutan giriş yapamaz.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final EmailVerificationService verificationService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    public AuthController(EmailVerificationService verificationService, PasswordResetService passwordResetService,
                          UserRepository userRepository)
    {
        this.verificationService = verificationService;
        this.passwordResetService = passwordResetService;
        this.userRepository = userRepository;
    }

    //Kodu doğrula: doğruysa 204; yanlışsa 400 {"error":"WRONG_CODE"}; kod yok/süresi dolmuş 400 {"error":"NO_ACTIVE_CODE"};
    //5 yanlış 429 (yeni kod istenmeli); zaten doğrulanmışsa 204 (tekrar basmak hata değil)
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto request,
                                                           @AuthenticationPrincipal Long userId)
    {
        return switch(verificationService.verify(userId, request.getCode()))
        {
            case VERIFIED, ALREADY_VERIFIED -> ResponseEntity.noContent().build();
            case WRONG_CODE -> ResponseEntity.badRequest().body(Map.of("error", "WRONG_CODE"));
            case NO_ACTIVE_CODE -> ResponseEntity.badRequest().body(Map.of("error", "NO_ACTIVE_CODE"));
            case TOO_MANY_ATTEMPTS -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "TOO_MANY_ATTEMPTS"));
        };
    }

    //Yeni kod gönder: 202; zaten doğrulanmışsa 409; çok sıksa 429 (TooManyRequestsException -> handler)
    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resendCode(@AuthenticationPrincipal Long userId)
    {
        User user = userRepository.findById(userId).orElse(null);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if(user.isEmailVerified())
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        verificationService.resendCode(user);
        return ResponseEntity.accepted().build();
    }

    //Her zaman 202, e-posta kayıtlı olsun olmasın: cevap, adresin sitede olup olmadığını söylemez
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request)
    {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    //Bağlantı geçerliyse şifre değişir (204), geçersiz/süresi dolmuş/kullanılmışsa 400
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request)
    {
        if(!passwordResetService.resetPassword(request.getToken(), request.getNewPassword()))
            return ResponseEntity.badRequest().build();
        return ResponseEntity.noContent().build();
    }
}
