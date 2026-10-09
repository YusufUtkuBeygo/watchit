package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.PublicUserResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.entity.User;
import org.mapstruct.Mapper;

//Nesneyi dto nesnesine çevirme işini otomatik yapmak için kullanılıyor
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(User user);
    //Başkasına gösterilecek hâli: e-posta alanı olmadığı için kopyalanmaz
    PublicUserResponseDto toPublicDto(User user);
    User toEntity(UserRequestDto dto);

}