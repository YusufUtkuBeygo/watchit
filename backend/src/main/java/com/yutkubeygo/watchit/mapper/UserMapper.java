package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

//Nesneyi dto nesnesine çevirme işini otomatik yapmak için kullanılıyor
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(User user);
    List<UserResponseDto> toDtoList(List<User> users);
    User toEntity(UserRequestDto dto);

}