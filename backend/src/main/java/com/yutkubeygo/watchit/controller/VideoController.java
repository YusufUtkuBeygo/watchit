package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    //db'ye yeni eleman eklenecek/post edilecek
    @PostMapping
    public VideoResponseDto createVideo(@RequestBody VideoRequestDto videoRequestDto)
    {
        return videoService.createVideo(videoRequestDto);
    }

    //db'den eleman getirilecek/get edilecek
    @GetMapping
    public List<VideoResponseDto> getAllVideos()
    {
        return videoService.getAllVideos();
    }

    @GetMapping("/{id}")
    public VideoResponseDto getVideoById(@PathVariable long id)
    {
        return videoService.getVideo(id);
    }

    //db'de hali hazirda var olan eleman uzerinde degisiklik yapilacak gero koyulacak/put
    @PutMapping
    public VideoResponseDto updateVideo(@PathVariable("id") Long id,@RequestBody VideoRequestDto videoRequestDto)
    {
        return videoService.updateVideo(id, videoRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteVideoById(@RequestParam long videoId)
    {
        videoService.deleteVideo(videoId);
    }


}
