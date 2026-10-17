package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.PublicUserResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.service.LoginAttemptService;
import com.yutkubeygo.watchit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;


    public UserController(UserService userService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }


    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request)
    {
            UserResponseDto user = userService.createUser(request);

            //E-posta ya da kullanıcı adı zaten kullanılıyorsa conflict (409) gönderir
            if(user == null)
                return ResponseEntity.status(HttpStatus.CONFLICT).build();

            return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    //Bütün kullanıcıları listeleyen GET /users yok: e-postaları toplu vermemek için kaldırıldı

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal Long userId)
    {
        UserResponseDto user = userService.getUserById(userId);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserResponseDto> getUserById(@PathVariable Long id)
    {
        //Başkasının bilgileri e-postasız döner
        PublicUserResponseDto user = userService.getPublicUserById(id);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();


        return ResponseEntity.ok(user);
    }



    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateUser(@Valid @RequestBody UserRequestDto newUser, @AuthenticationPrincipal Long userId)
    {
        UserResponseDto updatedUser = userService.updateUser(userId,newUser);

        if(updatedUser==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();//404

        return ResponseEntity.ok(updatedUser);//200 OK+Güncel veri
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUserById( @AuthenticationPrincipal Long userId)
    {
        //Eğer kullanıcı silinemediyse böyle bir kullanıcı yoktur/bulunamamıştır
        if(!userService.deleteUserById(userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request)
    {
        //Sayaç küçük harfli e-postaya göre: "A@x.com" ile "a@x.com" aynı hesap
        String email = UserService.normalizeEmail(request.getEmail());

        //Çok fazla yanlış deneme: şifre doğru olsa bile süre dolana kadar 429
        if(loginAttemptService.isBlocked(email))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

        LoginResponseDto logined = userService.login(request);

        if(logined == null)
        {
            loginAttemptService.recordFailure(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        loginAttemptService.recordSuccess(email);
        return ResponseEntity.ok(logined);
    }
}
