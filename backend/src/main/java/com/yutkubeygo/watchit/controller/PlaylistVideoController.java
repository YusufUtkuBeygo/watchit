package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PlaylistVideoRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistVideoResponseDto;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import com.yutkubeygo.watchit.service.PlaylistVideoService;
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
    public PlaylistVideoResponseDto getPlaylistVideoById(@PathVariable Long id)
    {
        return playlistVideoService.getPlaylistVideoById(id);
    }

    @PostMapping
    public PlaylistVideoResponseDto createPlaylistVideo(@RequestBody PlaylistVideoRequestDto playlistVideoRequestDto)
    {
        return  playlistVideoService.createPlaylistVideo(playlistVideoRequestDto);
    }

    @PutMapping("/{id}")
    public PlaylistVideoResponseDto updatePlaylistVideo(@PathVariable Long id, @RequestBody PlaylistVideoRequestDto playlistVideoRequestDto)
    {
        return playlistVideoService.updatePlaylistVideo(id,playlistVideoRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deletePlaylistVideoById(@PathVariable Long id)
    {
        playlistVideoService.deletePlaylistVideo(id);
    }

}
