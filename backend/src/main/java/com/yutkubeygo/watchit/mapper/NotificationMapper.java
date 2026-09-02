package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.NotificationRequestDto;
import com.yutkubeygo.watchit.dto.NotificationResponseDto;
import com.yutkubeygo.watchit.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel =  "spring")
public interface NotificationMapper {
    //todo acces modifier kullanimlari arastir
    @Mapping(source = "sender.id" , target ="senderId")
    @Mapping(source = "receiver.id" , target = "receiverId")
    public NotificationResponseDto toDto(Notification notification);
    public List<NotificationResponseDto> toDtoList(List<Notification> notifications);
    public Notification toEntity(NotificationRequestDto notificationRequestDto);


}
