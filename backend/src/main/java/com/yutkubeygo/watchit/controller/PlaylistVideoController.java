package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PlaylistVideoRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistVideoResponseDto;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import com.yutkubeygo.watchit.service.PlaylistVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/playlistvideos")
public class PlaylistVideoController {

    private final PlaylistVideoService playlistVideoService;

    public PlaylistVideoController(PlaylistVideoService playlistVideoService) {
        this.playlistVideoService = playlistVideoService;
    }

    @GetMapping
    public List<PlaylistVideoResponseDto> getAllPlaylistVideo()
    {
        return playlistVideoService.getAllPlaylistVideos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistVideoResponseDto> getPlaylistVideoById(@PathVariable Long id)
    {
        PlaylistVideoResponseDto playlistVideo = playlistVideoService.getPlaylistVideoById(id);

        if(playlistVideo == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(playlistVideo);
    }

    @PostMapping
    public ResponseEntity<PlaylistVideoResponseDto> createPlaylistVideo(@RequestBody PlaylistVideoRequestDto playlistVideoRequestDto)
    {
        PlaylistVideoResponseDto playlistVideo = playlistVideoService.createPlaylistVideo(playlistVideoRequestDto);

        //Servis null döndüyse istekteki playlist ya da video bulunamamıştır
        if(playlistVideo == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(playlistVideo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaylistVideoResponseDto> updatePlaylistVideo(@PathVariable Long id, @RequestBody PlaylistVideoRequestDto playlistVideoRequestDto)
    {
        PlaylistVideoResponseDto playlistVideo = playlistVideoService.updatePlaylistVideo(id,playlistVideoRequestDto);

        if(playlistVideo == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(playlistVideo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylistVideoById(@PathVariable Long id)
    {
        if(!playlistVideoService.deletePlaylistVideo(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
