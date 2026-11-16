package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.SubscriptionMapper;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.SubscriptionRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final NotificationService notificationService;

    public SubscriptionService(ChannelRepository channelRepository, UserRepository userRepository, SubscriptionRepository subscriptionRepository, SubscriptionMapper subscriptionMapper, NotificationService notificationService) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.notificationService = notificationService;
    }

    //Abonelik ve kanal sahibine giden bildirim tek transaction'da yazılır
    @Transactional
    public SubscriptionResponseDto createSubscription (SubscriptionRequestDto subscriptionRequestDto,Long userId)
    {
        Subscription subscription = subscriptionMapper.toEntity(subscriptionRequestDto);
        Channel channel = channelRepository.findById(subscriptionRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;
        if(channel.getOwner().getId().equals(userId))
            throw new ForbiddenException();

        User subscriber = userRepository.findById(userId).orElse(null);
        if(subscriber==null)
            return null;

        subscription.setChannel(channel);
        subscription.setSubscriber(subscriber);

        Subscription saved = subscriptionRepository.save(subscription);

        notificationService.notify(subscriber, channel.getOwner(), NotificationType.SUBSCRIBE,
                "Yeni abone", subscriber.getUsername() + " kanalına abone oldu.");

        return subscriptionMapper.toDto(saved);

    }

    public SubscriptionResponseDto getSubscriptionById(Long subscriptionId,Long userId)
    {

        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if(subscription==null)
            return null;

        if(!subscription.getSubscriber().getId().equals(userId))
            throw new ForbiddenException();

        return subscriptionMapper.toDto(subscription);

    }

    public List<SubscriptionResponseDto> getSubscriptionByUserId(Long userId)
    {
        return subscriptionMapper.toDtoList(subscriptionRepository.findBySubscriberId(userId));
    }

    //Update metodu yok: abonelik sadece abone ile kanal arasındaki ilişki, değişecek bir özelliği yok (çık + yeniden abone ol)

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Abonelik başkasınınsa ForbiddenException fırlatır (403)
    public boolean deleteMySubscription(Long userId,Long channelId)
    {
        Subscription subscription = subscriptionRepository.findBySubscriberIdAndChannelId(userId,channelId).orElse(null);
        if(subscription==null)
            return false;


        subscriptionRepository.delete(subscription);
        return true;
    }

}
