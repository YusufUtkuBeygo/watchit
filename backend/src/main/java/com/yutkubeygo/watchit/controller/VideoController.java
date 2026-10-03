package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<VideoResponseDto> createVideo(@Valid @RequestBody VideoRequestDto videoRequestDto)
    {
        VideoResponseDto video = videoService.createVideo(videoRequestDto);

        //Servis null döndüyse istekteki kanal ya da kategori bulunamamıştır
        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }

    //db'den eleman getirilecek/get edilecek
    @GetMapping
    public List<VideoResponseDto> getAllVideos()
    {
        return videoService.getAllVideos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDto> getVideoById(@PathVariable long id)
    {
        VideoResponseDto video = videoService.getVideo(id);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    //db'de hali hazirda var olan eleman uzerinde degisiklik yapilacak gero koyulacak/put
    @PutMapping("/{id}")
    public ResponseEntity<VideoResponseDto> updateVideo(@PathVariable("id") Long id,@Valid @RequestBody VideoRequestDto videoRequestDto)
    {
        VideoResponseDto video = videoService.updateVideo(id, videoRequestDto);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(@PathVariable long id)
    {
        if(!videoService.deleteVideo(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
