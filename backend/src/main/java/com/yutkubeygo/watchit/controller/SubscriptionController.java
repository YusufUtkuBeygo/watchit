package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public SubscriptionResponseDto createSubscription(@RequestBody SubscriptionRequestDto subscriptionRequestDto)
    {
        return subscriptionService.createSubscription(subscriptionRequestDto);
    }

    @PutMapping("/{id}")
    public SubscriptionResponseDto updateSubscription(@PathVariable Long id , @RequestBody SubscriptionRequestDto subscriptionRequestDto)
    {
        return subscriptionService.updateSubscription(id,subscriptionRequestDto);
    }

    @GetMapping("/{id}")
    public SubscriptionResponseDto getSubscriptionById(@PathVariable Long id)
    {
        return subscriptionService.getSubscriptionById(id);
    }

    @GetMapping
    public List<SubscriptionResponseDto> getAllSubscriptions()
    {
        return subscriptionService.getAllSubscriptions();
    }

    @DeleteMapping("/{id}")
    public void deleteSubscriptionById(@PathVariable Long id)
    {
        subscriptionService.deleteSubscriptionById(id);
    }


}
