package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeResponseDto;
import com.yutkubeygo.watchit.service.VideoLikeService;
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
    public VideoLikeResponseDto getVideoLikeById(@PathVariable Long id)
    {
        return videoLikeService.getVideoLikeById(id);
    }

    @GetMapping
    public List<VideoLikeResponseDto> getAllVideoLike()
    {
        return videoLikeService.getAllVideoLikes();
    }

    @PostMapping
    public VideoLikeResponseDto createVideLike(@RequestBody VideoLikeRequestDto videoLikeRequestDto)
    {
        return videoLikeService.createVideoLike(videoLikeRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteVideoLikeById(@PathVariable Long id)
    {
        videoLikeService.deleteVideoLikeById(id);
    }
}
