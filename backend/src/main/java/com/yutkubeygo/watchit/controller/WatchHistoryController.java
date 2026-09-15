package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.service.WatchHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchHistory")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    public WatchHistoryController(WatchHistoryService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }

    @PostMapping
    public WatchHistoryResponseDto createOrUploadWatchHistory(@RequestBody WatchHistoryRequestDto watchHistoryRequestDto)
    {
        return watchHistoryService.createOrUpdateWatchHistory(watchHistoryRequestDto);
    }

    @GetMapping("/{id}")
    public WatchHistoryResponseDto getWatchHistoryById(@PathVariable Long id)
    {
        return watchHistoryService.getWatchHistoryById(id);
    }

    @GetMapping
    public List<WatchHistoryResponseDto> getAllWatchHistory()
    {
        return watchHistoryService.getAllWatchHistory();
    }

    @DeleteMapping("/{id}")
    public void deleteWatchHistory(@RequestParam Long id)
    {
        watchHistoryService.deleteWatchHistoryById(id);
    }


}
