package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.PlaylistVideoRequestDto;
import com.yutkubeygo.watchit.dto.PlaylistVideoResponseDto;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaylistVideoMapper {

    @Mapping(source = "playlist.id", target = "playlistId")
    @Mapping(source = "video.id", target = "videoId")
    public PlaylistVideoResponseDto toDto(PlaylistVideo playlistVideo);
    public List<PlaylistVideoResponseDto> toDtoList(List<PlaylistVideo> playlistVideo);
    //Request DTO'sunda olmayan alanlar (id, ilişkiler, tarihler) serviste ya da veritabanında dolar
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public PlaylistVideo toEntity(PlaylistVideoRequestDto playlistVideoRequestDto);

}
