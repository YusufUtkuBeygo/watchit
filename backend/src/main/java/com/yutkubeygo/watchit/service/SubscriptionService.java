package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
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

    public SubscriptionResponseDto createSubscription (SubscriptionRequestDto subscriptionRequestDto)
    {
        Subscription subscription = subscriptionMapper.toEntity(subscriptionRequestDto);
        Channel channel = channelRepository.findById(subscriptionRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;

        User subscriber = userRepository.findById(subscriptionRequestDto.getSubscriberId()).orElse(null);
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

    public SubscriptionResponseDto updateSubscription (Long id, SubscriptionRequestDto subscriptionRequestDto)
    {
        Subscription subscription = subscriptionRepository.findById(id).orElse(null);
        if(subscription==null)
            return null;

        User subscriber = userRepository.findById(subscriptionRequestDto.getSubscriberId()).orElse(null);
        if(subscriber==null)
            return null;

        Channel channel = channelRepository.findById(subscriptionRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;

        subscription.setChannel(channel);
        subscription.setSubscriber(subscriber);

        return subscriptionMapper.toDto(subscriptionRepository.save(subscription));
    }

    public void deleteSubscriptionById(Long subscriptionId)
    {
        subscriptionRepository.deleteById(subscriptionId);
    }

}
