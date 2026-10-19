package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.PublicUserResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.BeanMapping;

//Nesneyi dto nesnesine çevirme işini otomatik yapmak için kullanılıyor
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toDto(User user);
    //Başkasına gösterilecek hâli: e-posta alanı olmadığı için kopyalanmaz
    PublicUserResponseDto toPublicDto(User user);
    //Request DTO'sunda olmayan alanlar (id, ilişkiler, tarihler) serviste ya da veritabanında dolar
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    User toEntity(UserRequestDto dto);

}