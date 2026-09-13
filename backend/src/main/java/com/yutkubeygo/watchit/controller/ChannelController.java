package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ChannelRequestDto;
import com.yutkubeygo.watchit.dto.ChannelResponseDto;
import com.yutkubeygo.watchit.service.ChannelService;
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
    public ChannelResponseDto createChannel(@RequestBody ChannelRequestDto request)
    {
        return channelService.createChannel(request);
    }

    @GetMapping
    public List<ChannelResponseDto> getAllChanneles()
    {
        return channelService.getAllChannels();
    }

    @GetMapping("/{id}")
    public ChannelResponseDto getChannel(@PathVariable Long id)
    {
        return channelService.getChannelById(id);
    }

    @PutMapping("/{id}")
    public ChannelResponseDto updateChannel(@PathVariable Long id,@RequestBody ChannelRequestDto request)
    {
        return channelService.updateChannel(id,request);
    }

    @DeleteMapping("/{id}")
    public void deleteChannel(@PathVariable Long id)
    {
        channelService.deleteChannelById(id);
    }
}
