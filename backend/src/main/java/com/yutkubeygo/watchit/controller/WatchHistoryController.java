package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.service.WatchHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<WatchHistoryResponseDto> createOrUploadWatchHistory(@Valid @RequestBody WatchHistoryRequestDto watchHistoryRequestDto)
    {
        WatchHistoryResponseDto watchHistory = watchHistoryService.createOrUpdateWatchHistory(watchHistoryRequestDto);

        //Servis null döndüyse istekteki kullanıcı ya da video bulunamamıştır
        if(watchHistory == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(watchHistory);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchHistoryResponseDto> getWatchHistoryById(@PathVariable Long id)
    {
        WatchHistoryResponseDto watchHistory = watchHistoryService.getWatchHistoryById(id);

        if(watchHistory == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(watchHistory);
    }

    @GetMapping
    public List<WatchHistoryResponseDto> getAllWatchHistory()
    {
        return watchHistoryService.getAllWatchHistory();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchHistory(@PathVariable Long id)
    {
        if(!watchHistoryService.deleteWatchHistoryById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
