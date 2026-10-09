package com.yutkubeygo.watchit.controller;

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

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    //Veri tabanına yeni eleman(kanal) eklemek için createChannel servisini kullanıyoruz
    @PostMapping
    public ResponseEntity<ChannelResponseDto> createChannel(@Valid @RequestBody ChannelRequestDto request,@AuthenticationPrincipal Long userId)
    {
        ChannelResponseDto channel = channelService.createChannel(request,userId);

        //Servis null döndüyse istekteki owner (kullanıcı) bulunamamıştır
        if(channel == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(channel);
    }

    @GetMapping
    public List<ChannelResponseDto> getAllChanneles()
    {
        return channelService.getAllChannels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponseDto> getChannel(@PathVariable Long id)
    {
        ChannelResponseDto channel = channelService.getChannelById(id);

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
