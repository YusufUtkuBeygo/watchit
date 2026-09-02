package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.PlaylistVideoRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistVideoResponseDto;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaylistVideoMapper {

    @Mapping(source = "playlist.id", target = "playlistId")
    @Mapping(source = "video.id", target = "videoId")
    public PlaylistVideoResponseDto toDto(PlaylistVideo playlistVideo);
    public List<PlaylistVideoResponseDto> toDtoList(List<PlaylistVideo> playlistVideo);
    public PlaylistVideo toEntity(PlaylistVideoRequestDto playlistVideoRequestDto);

}
