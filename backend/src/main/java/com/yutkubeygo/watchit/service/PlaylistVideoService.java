package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PlaylistVideoRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistVideoResponseDto;
import com.yutkubeygo.watchit.entity.Playlist;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.PlaylistVideoMapper;
import com.yutkubeygo.watchit.repository.PlaylistVideoRepository;
import com.yutkubeygo.watchit.repository.PlaylistRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistVideoService
{
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;
    private final PlaylistVideoMapper playlistVideoMapper;
    private final PlaylistVideoRepository playlistVideoRepository;

    public PlaylistVideoService(PlaylistRepository playlistRepository, VideoRepository videoRepository, PlaylistVideoMapper playlistVideoMapper, PlaylistVideoRepository playlistVideoRepository) {
        this.playlistRepository = playlistRepository;
        this.videoRepository = videoRepository;
        this.playlistVideoMapper = playlistVideoMapper;
        this.playlistVideoRepository = playlistVideoRepository;
    }

    //Playlist başkasınınsa ForbiddenException fırlatır (403)
    public PlaylistVideoResponseDto createPlaylistVideo(PlaylistVideoRequestDto playlistVideoRequestDto, Long userId)
    {


        Playlist playlist = playlistRepository.findById(playlistVideoRequestDto.getPlaylistId()).orElse(null);
        if(playlist == null)
            return null;

        Long ownerId = playlist.getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        Video video = videoRepository.findById(playlistVideoRequestDto.getVideoId()).orElse(null);
        if(video == null)
            return null;

        PlaylistVideo playlistVideo = playlistVideoMapper.toEntity(playlistVideoRequestDto);

        playlistVideo.setPlaylist(playlist);
        playlistVideo.setVideo(video);

        return playlistVideoMapper.toDto(playlistVideoRepository.save(playlistVideo));

    }


    public PlaylistVideoResponseDto getPlaylistVideoById(Long id)
    {
        PlaylistVideo playlistVideo = playlistVideoRepository.findById(id).orElse(null);
        if(playlistVideo == null)
            return null;

        return playlistVideoMapper.toDto(playlistVideo);
    }

    public List<PlaylistVideoResponseDto> getAllPlaylistVideos()
    {
        return playlistVideoMapper.toDtoList(playlistVideoRepository.findAll());
    }

    //Kaydın şimdiki playlisti ya da taşınacağı playlist başkasınınsa ForbiddenException fırlatır (403)
    public PlaylistVideoResponseDto updatePlaylistVideo(Long id, PlaylistVideoRequestDto playlistVideoRequestDto, Long userId)
    {
        PlaylistVideo playlistVideo = playlistVideoRepository.findById(id).orElse(null);
        if(playlistVideo == null)
            return null;

        Long ownerId = playlistVideo.getPlaylist().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        Playlist playlist = playlistRepository.findById(playlistVideoRequestDto.getPlaylistId()).orElse(null);
        if(playlist == null)
            return null;

        //Taşınacağı playlist de kullanıcının olmalı
        Long newOwnerId = playlist.getOwner().getId();
        if(!newOwnerId.equals(userId))
            throw new ForbiddenException();

        Video video = videoRepository.findById(playlistVideoRequestDto.getVideoId()).orElse(null);
        if(video == null)
            return null;

        playlistVideo.setPlaylist(playlist);
        playlistVideo.setVideo(video);
        playlistVideo.setOrderIndex(playlistVideoRequestDto.getOrderIndex());

        return playlistVideoMapper.toDto(playlistVideoRepository.save(playlistVideo));
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Playlist başkasınınsa ForbiddenException fırlatır (403)
    public boolean deletePlaylistVideo(Long id, Long userId)
    {
        PlaylistVideo playlistVideo = playlistVideoRepository.findById(id).orElse(null);
        if(playlistVideo == null)
            return false;

        Long ownerId = playlistVideo.getPlaylist().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        playlistVideoRepository.delete(playlistVideo);
        return true;
    }
}
