package com.yutkubeygo.watchit.service;
import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.mapper.UserMapper;
import  com.yutkubeygo.watchit.repository.UserRepository;
import  com.yutkubeygo.watchit.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import  org.springframework.stereotype.Service;

import java.util.List;


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
        if(userRepository.existsByEmail(request.getEmail()))
            return null;
        //Dışardan gelen json user entitiysinin bir nesnesine dönüştürülür
        User user = userMapper.toEntity(request);
        //şifre hashleniyor
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //Nesne oluşturulduktan sonra dto'ya çevirip return etmek için savedUser' atıyoruz elimizdeklieri
        User savedUser=userRepository.save(user);
        //dto nesnesini return ediyoruz içeriğini biz ayarladıkS
        return userMapper.toDto(savedUser);
    }

    public  List<UserResponseDto> getAllUsers()
    {
        List<User> userList= userRepository.findAll();
        return userMapper.toDtoList(userList);
    }

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


        user.setUsername(newUser.getUsername());
        user.setEmail(newUser.getEmail());
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
        User loginUser = userRepository.findByEmail(request.getEmail()).orElse(null);

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

}
