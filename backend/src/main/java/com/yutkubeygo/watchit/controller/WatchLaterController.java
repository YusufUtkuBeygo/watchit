package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.dto.WatchLaterRequestDto;
import com.yutkubeygo.watchit.service.WatchLaterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//"Daha sonra izle" listesi. Hepsi giriş ister (SecurityConfig: anyRequest().authenticated())
@RestController
@RequestMapping("/watchLater")
public class WatchLaterController {

    private final WatchLaterService watchLaterService;

    public WatchLaterController(WatchLaterService watchLaterService)
    {
        this.watchLaterService = watchLaterService;
    }

    //Listemdeki videolar, son eklenen en üstte
    @GetMapping
    public PageResponseDto<VideoResponseDto> getMyList(@AuthenticationPrincipal Long userId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size)
    {
        return watchLaterService.getMyList(userId, Paging.of(page, size));
    }

    //Eklendiyse 201, zaten listedeyse 200, video yoksa 404
    @PostMapping
    public ResponseEntity<Void> add(@Valid @RequestBody WatchLaterRequestDto request, @AuthenticationPrincipal Long userId)
    {
        return switch(watchLaterService.add(userId, request.getVideoId()))
        {
            case ADDED -> ResponseEntity.status(HttpStatus.CREATED).build();
            case ALREADY_SAVED -> ResponseEntity.ok().build();
            case VIDEO_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        };
    }

    //Listeden çıkar: 204, listede yoksa 404
    @DeleteMapping
    public ResponseEntity<Void> remove(@RequestParam Long videoId, @AuthenticationPrincipal Long userId)
    {
        if(!watchLaterService.remove(userId, videoId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }
}
