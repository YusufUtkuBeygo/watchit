package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeResponseDto;
import com.yutkubeygo.watchit.entity.VideoLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoLikeMapper {

    @Mapping(source = "user.id", target="userId")
    @Mapping(source = "video.id", target="videoId")
    public VideoLikeResponseDto toDto(VideoLike videoLike);
    public List<VideoLikeResponseDto> toDtoList(List<VideoLike> videoLikes);
    public VideoLike toEntity(VideoLikeRequestDto videoLikeRequestDto);

}
