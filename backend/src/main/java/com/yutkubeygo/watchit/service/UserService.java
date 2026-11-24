package com.yutkubeygo.watchit.service;
import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.PasswordChangeRequestDto;
import com.yutkubeygo.watchit.dto.PublicUserResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.dto.UserUpdateRequestDto;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.mapper.UserMapper;
import  com.yutkubeygo.watchit.repository.UserRepository;
import  com.yutkubeygo.watchit.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import  org.springframework.stereotype.Service;

import com.yutkubeygo.watchit.repository.EmailTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class UserService {

    private final  UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final LoginAttemptService loginAttemptService;

    private final EmailVerificationService emailVerificationService;

    private final EmailTokenRepository emailTokenRepository;

//    public UserService(UserRepository userRepository, UserMapper userMapper) {
//        this.userRepository = userRepository;
//        this.userMapper = userMapper;
//    }

    public  UserResponseDto createUser(UserRequestDto request)
    {
        //E-posta küçük harfle, kenar boşlukları kırpılarak saklanır: "Ayse@X.com " ile "ayse@x.com" aynı hesap
        String email = normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();

        if(userRepository.existsByEmail(email))
            return null;
        //Kullanıcı adı da tek olmalı (V5: veritabanında da benzersiz); null -> controller 409 döner
        if(userRepository.existsByUsernameIgnoreCase(username))
            return null;
        //Dışardan gelen json user entitiysinin bir nesnesine dönüştürülür
        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setUsername(username);
        //şifre hashleniyor
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //Nesne oluşturulduktan sonra dto'ya çevirip return etmek için savedUser' atıyoruz elimizdeklieri
        User savedUser=userRepository.save(user);
        //Yeni hesap doğrulanmamış başlar: e-postaya 6 haneli kod gider (arka planda; gönderilemese de kayıt olur)
        emailVerificationService.sendCodeQuietly(savedUser);
        //dto nesnesini return ediyoruz içeriğini biz ayarladıkS
        return userMapper.toDto(savedUser);
    }

    //Başka bir kullanıcının herkese açık bilgileri (e-posta yok)
    public PublicUserResponseDto getPublicUserById(Long id)
    {
        User user = userRepository.findById(id).orElse(null);

        if(user==null)
            return null;

        return userMapper.toPublicDto(user);
    }

    //Kullanıcının kendi bilgileri (e-posta dahil), /users/me için
    public UserResponseDto getUserById(Long id)
    {

        User user = userRepository.findById(id).orElse(null);

        if(user==null)
            return null;

//        UserResponseDto dto=new UserResponseDto();
//
//        dto.setUsername(user.getUsername());
//        dto.setEmail(user.getEmail());
//        dto.setId(user.getId());
//
//        return dto;

        return userMapper.toDto(user);


    }

    //Yalnız ad ve e-posta değişir. E-posta değişiyorsa şu anki şifre doğru olmalı (yanlışsa 403, çok denemede 429).
    //Kullanıcı adı ya da e-posta başkasındaysa veritabanının benzersizlik kuralı 409 verir.
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto newUser)
    {
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
            return null;

        String newEmail = normalizeEmail(newUser.getEmail());
        boolean emailChanged = !newEmail.equals(user.getEmail());
        if(emailChanged)
            checkCurrentPassword(user, newUser.getCurrentPassword());

        user.setUsername(newUser.getUsername().trim());
        user.setEmail(newEmail);
        //Yeni adresin gerçekten bu kişiye ait olduğu bilinmiyor: yeniden doğrulanana kadar doğrulanmamış
        if(emailChanged)
            user.setEmailVerified(false);

        User savedUser=userRepository.save(user);
        if(emailChanged)
            emailVerificationService.sendCodeQuietly(savedUser);

        return userMapper.toDto(savedUser);
    }

    //Şifre değiştirme: şu anki şifre doğru olmalı. Kullanıcı yoksa null (404).
    //Şifre değişince bütün eski oturumlar kapanır (passwordChangedAt); bu oturum kopmasın diye yeni token döner.
    public String changePassword(Long id, PasswordChangeRequestDto request)
    {
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
            return null;

        checkCurrentPassword(user, request.getCurrentPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    //Şu anki şifreyi doğrular. Girişteki sayaç kullanılır: token'ı çalan biri burada şifre tahmini de yapamasın.
    private void checkCurrentPassword(User user, String currentPassword)
    {
        if(loginAttemptService.isBlocked(user.getEmail()))
            throw new TooManyRequestsException();

        if(currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword()))
        {
            loginAttemptService.recordFailure(user.getEmail());
            throw new ForbiddenException();
        }

        loginAttemptService.recordSuccess(user.getEmail());
    }

    //Hesabı siler. Kodlar önce açıkça silinir: veritabanı ON DELETE CASCADE ile zaten silerdi, ama aynı işlem içinde
    //Hibernate'in hafızasında bir kod kaydı kalmışsa "sahibi silinmiş kayıt" diye işlemi durdururdu.
    @Transactional
    public boolean deleteUserById(Long id)
    {
      if(!userRepository.existsById(id))
          return false;

      emailTokenRepository.deleteByUserId(id);
      userRepository.deleteById(id);
      return true;
    }

    public LoginResponseDto login(LoginRequestDto request)
    {
        //requestin içindeki e-postayla kullanıcıyı bul
        User loginUser = userRepository.findByEmail(normalizeEmail(request.getEmail())).orElse(null);

        if(loginUser==null)
            return null;

        //requestteki şifre ile db'deki şifreyi passwordEncoder ile karşılaştır
        if(!passwordEncoder.matches(request.getPassword(),loginUser.getPassword()))
            return null;

        //Doğrulamadan geçerse kullanıcıyı  dto'ya  çevirip geri dön
        UserResponseDto userResponseDto = userMapper.toDto(loginUser);

        String token = jwtService.generateToken(loginUser);

        LoginResponseDto loginResponseDto = new LoginResponseDto();
        loginResponseDto.setToken(token);
        loginResponseDto.setUser(userResponseDto);
        return loginResponseDto;

    }


    //E-postayı tek biçime getirir: kenar boşlukları kırpılır, küçük harfe çevrilir.
    //Locale.ROOT şart: Türkçe ayarlı bir bilgisayarda "I".toLowerCase() "ı" olur ve "INFO@x.com" bozulurdu
    public static String normalizeEmail(String email)
    {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
