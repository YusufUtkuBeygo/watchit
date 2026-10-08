package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeResponseDto;
import com.yutkubeygo.watchit.service.VideoLikeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videoLikes")
public class VideoLikeController {

    private final VideoLikeService videoLikeService;

    public VideoLikeController(VideoLikeService videoLikeService) {
        this.videoLikeService = videoLikeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoLikeResponseDto> getVideoLikeById(@PathVariable Long id)
    {
        VideoLikeResponseDto videoLike = videoLikeService.getVideoLikeById(id);

        if(videoLike == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(videoLike);
    }

    @GetMapping
    public List<VideoLikeResponseDto> getAllVideoLike()
    {
        return videoLikeService.getAllVideoLikes();
    }

    @PostMapping
    public ResponseEntity<VideoLikeResponseDto> createVideLike(@Valid @RequestBody VideoLikeRequestDto videoLikeRequestDto,@AuthenticationPrincipal Long userId)
    {
        VideoLikeResponseDto videoLike = videoLikeService.createVideoLike(videoLikeRequestDto,userId);

        //Servis null döndüyse istekteki kullanıcı ya da video bulunamamıştır
        if(videoLike == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(videoLike);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoLikeById(@PathVariable Long id)
    {
        if(!videoLikeService.deleteVideoLikeById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
