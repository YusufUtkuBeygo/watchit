package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.SubscriptionMapper;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.SubscriptionRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionService(ChannelRepository channelRepository, UserRepository userRepository, SubscriptionRepository subscriptionRepository, SubscriptionMapper subscriptionMapper) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    public SubscriptionResponseDto createSubscription (SubscriptionRequestDto subscriptionRequestDto,Long userId)
    {
        Subscription subscription = subscriptionMapper.toEntity(subscriptionRequestDto);
        Channel channel = channelRepository.findById(subscriptionRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;

        User subscriber = userRepository.findById(userId).orElse(null);
        if(subscriber==null)
            return null;

        subscription.setChannel(channel);
        subscription.setSubscriber(subscriber);

        return subscriptionMapper.toDto(subscriptionRepository.save(subscription));

    }

    public SubscriptionResponseDto getSubscriptionById(Long subscriptionId)
    {

        return subscriptionMapper.toDto(subscriptionRepository.findById(subscriptionId).orElse(null));

    }

    public List<SubscriptionResponseDto> getAllSubscriptions()
    {
        return subscriptionMapper.toDtoList(subscriptionRepository.findAll());
    }

    //Update metodu yok: abonelik sadece abone ile kanal arasındaki ilişki, değişecek bir özelliği yok (çık + yeniden abone ol)

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Abonelik başkasınınsa ForbiddenException fırlatır (403)
    public boolean deleteSubscriptionById(Long subscriptionId, Long userId)
    {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if(subscription==null)
            return false;

        Long ownerId = subscription.getSubscriber().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        subscriptionRepository.deleteById(subscriptionId);
        return true;
    }

}
