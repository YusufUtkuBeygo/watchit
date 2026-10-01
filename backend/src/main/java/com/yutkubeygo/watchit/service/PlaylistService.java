package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PlaylistRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistResponseDto;
import com.yutkubeygo.watchit.entity.Playlist;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.mapper.PlaylistMapper;
import com.yutkubeygo.watchit.repository.PlaylistRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistMapper playlistMapper;
    private final UserRepository userRepository;


    public PlaylistService(PlaylistRepository playlistRepository, PlaylistMapper playListMapper, UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistMapper = playListMapper;
        this.userRepository = userRepository;
    }


    public PlaylistResponseDto createPlaylist(PlaylistRequestDto playlistRequestDto)
    {
        User user = userRepository.findById(playlistRequestDto.getOwnerId()).orElse(null);
        if(user==null)
            return null;

        Playlist playlist = playlistMapper.toEntity(playlistRequestDto);
        playlist.setOwner(user);
        return playlistMapper.toDto(playlistRepository.save(playlist));
    }

    public PlaylistResponseDto updatePlaylist (Long id, PlaylistRequestDto playlistRequestDto)
    {
        Playlist playlist = playlistRepository.findById(id).orElse(null);
        if(playlist==null)
            return null;

        User user = userRepository.findById(playlistRequestDto.getOwnerId()).orElse(null);
        if(user==null)
            return null;

        playlist.setTitle(playlistRequestDto.getTitle());
        playlist.setDescription(playlistRequestDto.getDescription());
        playlist.setIsPublic(playlistRequestDto.getIsPublic());
        playlist.setOwner(user);

        return playlistMapper.toDto(playlistRepository.save(playlist));
    }

    public PlaylistResponseDto getPlaylistById(Long id)
    {
        Playlist playlist = playlistRepository.findById(id).orElse(null);
        if(playlist==null)
            return null;
        return playlistMapper.toDto(playlist);
    }

    public List<PlaylistResponseDto> getAllPlaylists()
    {
        List<Playlist> playlist = playlistRepository.findAll();
        return playlistMapper.toDtoList(playlist);
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deletePlaylist(Long id)
    {
        if(!playlistRepository.existsById(id))
            return false;

        playlistRepository.deleteById(id);
        return true;
    }

}
