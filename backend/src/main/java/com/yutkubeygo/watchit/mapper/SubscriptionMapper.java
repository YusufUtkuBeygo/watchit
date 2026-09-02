package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(source = "subscriber.id", target = "subscriberId")
    @Mapping(source = "channel.id", target = "channelId")
    public SubscriptionResponseDto toDto(Subscription subscription);
    public List<SubscriptionResponseDto> toDtoList(List<Subscription> subscriptions);
    public Subscription toEntity(SubscriptionRequestDto subscriptionRequestDto);


}
