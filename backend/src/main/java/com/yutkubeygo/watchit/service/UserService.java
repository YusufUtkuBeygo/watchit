package com.yutkubeygo.watchit.service;
import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.PublicUserResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.mapper.UserMapper;
import  com.yutkubeygo.watchit.repository.UserRepository;
import  com.yutkubeygo.watchit.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import  org.springframework.stereotype.Service;

import java.util.Locale;


@Service
@RequiredArgsConstructor
public class UserService {

    private final  UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

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

    public UserResponseDto updateUser(Long id,UserRequestDto newUser)
    {
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
            return null;


        user.setUsername(newUser.getUsername().trim());
        user.setEmail(normalizeEmail(newUser.getEmail()));
        //şifre hashleniyor
        user.setPassword(passwordEncoder.encode(newUser.getPassword()));

        User savedUser=userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    public boolean deleteUserById(Long id)
    {
      if(!userRepository.existsById(id))
          return false;

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
