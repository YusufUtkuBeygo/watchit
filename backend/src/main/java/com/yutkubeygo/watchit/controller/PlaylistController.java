package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PlaylistRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistResponseDto;
import com.yutkubeygo.watchit.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<PlaylistResponseDto>  createPlaylist(@Valid @RequestBody PlaylistRequestDto playlistRequestDto,@AuthenticationPrincipal Long userId)
    {
        PlaylistResponseDto playlist = playlistService.createPlaylist(playlistRequestDto,userId);

        //Servis null döndüyse istekteki owner (kullanıcı) bulunamamıştır
        if(playlist == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponseDto> getPlaylist(@PathVariable Long id )
    {
        PlaylistResponseDto playlist = playlistService.getPlaylistById(id);

        if(playlist == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(playlist);
    }

    @GetMapping
    public List<PlaylistResponseDto> getAllPlaylists()
    {
        return playlistService.getAllPlaylists();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaylistResponseDto> updatePlaylist(@PathVariable Long id,@Valid @RequestBody PlaylistRequestDto playlistRequestDto,@AuthenticationPrincipal Long userId)
    {
        PlaylistResponseDto playlist = playlistService.updatePlaylist(id,playlistRequestDto,userId);

        if(playlist == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(playlist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable Long id,@AuthenticationPrincipal Long userId)
    {
        if(!playlistService.deletePlaylist(id,userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
