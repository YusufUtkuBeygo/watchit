package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.SubscriptionResponseDto;
import com.yutkubeygo.watchit.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<SubscriptionResponseDto> createSubscription(@Valid @RequestBody SubscriptionRequestDto subscriptionRequestDto,@AuthenticationPrincipal Long userId)
    {
        SubscriptionResponseDto subscription = subscriptionService.createSubscription(subscriptionRequestDto,userId);

        //Servis null döndüyse istekteki kanal ya da abone olacak kullanıcı bulunamamıştır
        if(subscription == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> getSubscriptionById(@PathVariable Long id)
    {
        SubscriptionResponseDto subscription = subscriptionService.getSubscriptionById(id);

        if(subscription == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(subscription);
    }

    @GetMapping
    public List<SubscriptionResponseDto> getAllSubscriptions()
    {
        return subscriptionService.getAllSubscriptions();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriptionById(@PathVariable Long id)
    {
        if(!subscriptionService.deleteSubscriptionById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
