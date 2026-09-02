package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.PlaylistRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistResponseDto;
import com.yutkubeygo.watchit.entity.Playlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    public PlaylistResponseDto toDto(Playlist playlist);
    public List<PlaylistResponseDto> toDtoList(List <Playlist> playlist);
    public Playlist toEntity(PlaylistRequestDto playlistRequestDto);
}
