package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.service.WatchHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchHistory")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    public WatchHistoryController(WatchHistoryService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }

    //Bu metot hem oluşturuyor hem güncelliyor, hangisini yaptığını bilmediğimiz için 201 yerine 200 dönüyoruz
    @PostMapping
    public ResponseEntity<WatchHistoryResponseDto> createOrUploadWatchHistory(@Valid @RequestBody WatchHistoryRequestDto watchHistoryRequestDto,@AuthenticationPrincipal Long userId)
    {
        WatchHistoryResponseDto watchHistory = watchHistoryService.createOrUpdateWatchHistory(watchHistoryRequestDto,userId);

        //Servis null döndüyse istekteki kullanıcı ya da video bulunamamıştır
        if(watchHistory == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(watchHistory);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchHistoryResponseDto> getWatchHistoryById(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        WatchHistoryResponseDto watchHistory = watchHistoryService.getWatchHistoryById(id,userId);

        if(watchHistory == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(watchHistory);
    }

    @GetMapping
    //?videoId=5 verilirse yalnız o videonun kaydım (0 ya da 1 kayıt; oynatıcı "kaldığın yerden devam" için)
    public PageResponseDto<WatchHistoryResponseDto> getAllWatchHistory(@AuthenticationPrincipal Long userId,
                                                                       @RequestParam(required = false) Long videoId,
                                                                       @RequestParam(required = false) Integer page,
                                                                       @RequestParam(required = false) Integer size)
    {
        if(videoId != null)
            return watchHistoryService.getMyEntryForVideo(userId, videoId);
        return watchHistoryService.getWatchHistoryByUserId(userId, Paging.of(page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchHistory(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        if(!watchHistoryService.deleteWatchHistoryById(id,userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
