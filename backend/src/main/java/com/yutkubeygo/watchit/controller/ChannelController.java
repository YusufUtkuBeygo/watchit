package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.service.EmailVerificationService;

import com.yutkubeygo.watchit.dto.ChannelRequestDto;
import com.yutkubeygo.watchit.dto.ChannelResponseDto;
import com.yutkubeygo.watchit.service.ChannelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/channels")
public class ChannelController {

    private final ChannelService channelService;
    //E-postası doğrulanmamış hesap bu işi yapamaz (403 EMAIL_NOT_VERIFIED)
    private final EmailVerificationService emailVerificationService;

    public ChannelController(ChannelService channelService, EmailVerificationService emailVerificationService) {
        this.channelService = channelService;
        this.emailVerificationService = emailVerificationService;
    }

    //Veri tabanına yeni eleman(kanal) eklemek için createChannel servisini kullanıyoruz
    @PostMapping
    public ResponseEntity<ChannelResponseDto> createChannel(@Valid @RequestBody ChannelRequestDto request,@AuthenticationPrincipal Long userId)
    {
        emailVerificationService.requireVerified(userId);
        ChannelResponseDto channel = channelService.createChannel(request,userId);

        //Servis null döndüyse istekteki owner (kullanıcı) bulunamamıştır
        if(channel == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(channel);
    }

    @GetMapping
    public List<ChannelResponseDto> getAllChannels()
    {
        return channelService.getAllChannels();
    }

    //Giriş yapan kullanıcının kanalı; kanalı yoksa 404 (frontend "önce kanal oluştur" der)
    //GET /channels/me/stats?days=28 (7-90): Stüdyo grafiği. Kanalım yoksa 404
    @GetMapping("/me/stats")
    public ResponseEntity<com.yutkubeygo.watchit.dto.ChannelStatsDto> getMyStats(@AuthenticationPrincipal Long userId,
                                                                               @RequestParam(defaultValue = "28") int days)
    {
        if(days < 7 || days > 90)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        com.yutkubeygo.watchit.dto.ChannelStatsDto stats = channelService.getMyStats(userId, days);
        if(stats == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/me")
    public ResponseEntity<ChannelResponseDto> getMyChannel(@AuthenticationPrincipal Long userId)
    {
        ChannelResponseDto channel = channelService.getMyChannel(userId);

        if(channel == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(channel);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponseDto> getChannel(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        ChannelResponseDto channel = channelService.getChannelById(id,userId);

        if(channel == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(channel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponseDto> updateChannel(@PathVariable Long id,@Valid @RequestBody ChannelRequestDto request,@AuthenticationPrincipal Long userId)
    {
        ChannelResponseDto channel = channelService.updateChannel(id,request,userId);

        if(channel == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(channel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        if(!channelService.deleteChannelById(id,userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
