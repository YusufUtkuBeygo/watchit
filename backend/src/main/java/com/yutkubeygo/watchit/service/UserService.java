package com.yutkubeygo.watchit.service;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.mapper.UserMapper;
import  com.yutkubeygo.watchit.repository.UserRepository;
import  com.yutkubeygo.watchit.entity.User;
import lombok.RequiredArgsConstructor;
import  org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {

    private final  UserRepository userRepository;

    private final UserMapper userMapper;

//    public UserService(UserRepository userRepository, UserMapper userMapper) {
//        this.userRepository = userRepository;
//        this.userMapper = userMapper;
//    }

    public  UserResponseDto createUser(UserRequestDto request)
    {
        //Dışardan gelen json user entitiysinin bir nesnesine dönüştürülür
        User user = userMapper.toEntity(request);
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

    public UserResponseDto updateUser(Long id,User newUser)
    {
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
            return null;


        user.setUsername(newUser.getUsername());
        user.setEmail(newUser.getEmail());
        user.setPassword(newUser.getPassword());

        User savedUser=userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    public void deleteUserById(Long id)
    {
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
         return;   //404

        userRepository.deleteById(id);
    }

}
