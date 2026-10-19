package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel =  "spring")
public interface NotificationMapper {
    //todo acces modifier kullanimlari arastir
    @Mapping(source = "sender.id" , target ="senderId")
    @Mapping(source = "receiver.id" , target = "receiverId")
    public NotificationResponseDto toDto(Notification notification);
    public List<NotificationResponseDto> toDtoList(List<Notification> notifications);
    //Request DTO'sunda olmayan alanlar (id, ilişkiler, tarihler) serviste ya da veritabanında dolar
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public Notification toEntity(NotificationRequestDto notificationRequestDto);


}
