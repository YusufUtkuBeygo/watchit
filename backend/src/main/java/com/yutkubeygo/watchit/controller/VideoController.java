package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<VideoResponseDto> createVideo(@Valid @RequestBody VideoRequestDto videoRequestDto,@AuthenticationPrincipal Long userId)
    {
        VideoResponseDto video = videoService.createVideo(videoRequestDto,userId);

        //Servis null döndüyse istekteki kanal ya da kategori bulunamamıştır
        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }


    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id, @AuthenticationPrincipal Long userId, HttpServletRequest request)
    {
        String ip = request.getRemoteAddr();
        if(videoService.recordView(id,userId,ip))
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.notFound().build();

    }

    //db'den eleman getirilecek/get edilecek
    //GET /videos -> bütün videolar, GET /videos?channelId=3 -> yalnız o kanalın videoları,
    //GET /videos?categoryId=2 -> yalnız o kategorinin videoları
    @GetMapping
    public ResponseEntity<List<VideoResponseDto>> getAllVideos(@RequestParam(required = false) Long channelId,
                                                               @RequestParam(required = false) Long categoryId)
    {
        //İkisi de verilmediyse eskisi gibi hepsi
        if(channelId == null && categoryId == null)
            return ResponseEntity.ok(videoService.getAllVideos());

        List<VideoResponseDto> videos = channelId != null
                ? videoService.getVideosByChannelId(channelId)
                : videoService.getVideosByCategoryId(categoryId);

        //Servis null döndüyse kanal ya da kategori yoktur
        if(videos == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDto> getVideoById(@PathVariable long id,@AuthenticationPrincipal Long userId)
    {
        VideoResponseDto video = videoService.getVideo(id,userId);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    //db'de hali hazirda var olan eleman uzerinde degisiklik yapilacak gero koyulacak/put
    @PutMapping("/{id}")
    public ResponseEntity<VideoResponseDto> updateVideo(@PathVariable("id") Long id,@Valid @RequestBody VideoRequestDto videoRequestDto,@AuthenticationPrincipal Long userId)
    {
        VideoResponseDto video = videoService.updateVideo(id, videoRequestDto, userId);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(@PathVariable long id, @AuthenticationPrincipal Long userId)
    {
        if(!videoService.deleteVideo(id, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
