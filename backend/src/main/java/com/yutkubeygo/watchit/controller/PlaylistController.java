package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PlaylistRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistResponseDto;
import com.yutkubeygo.watchit.service.PlaylistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping
    public PlaylistResponseDto  createPlaylist(@RequestBody PlaylistRequestDto playlistRequestDto)
    {
        return playlistService.createPlaylist(playlistRequestDto);
    }

    @GetMapping("/{id}")
    public PlaylistResponseDto getPlaylist(@PathVariable Long id )
    {
        return playlistService.getPlaylistById(id);
    }

    @GetMapping
    public List<PlaylistResponseDto> getAllPlaylists()
    {
        return playlistService.getAllPlaylists();
    }

    @PutMapping("/{id}")
    public PlaylistResponseDto updatePlaylist(@PathVariable Long id,@RequestBody PlaylistRequestDto playlistRequestDto)
    {
        return playlistService.updatePlaylist(id,playlistRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deletePlaylist(@PathVariable Long id)
    {
        playlistService.deletePlaylist(id);
    }

}
