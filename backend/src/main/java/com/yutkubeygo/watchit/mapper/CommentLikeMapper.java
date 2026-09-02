package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.CommentLikeRequestDto;
import com.yutkubeygo.watchit.dto.CommentLikeResponseDto;
import com.yutkubeygo.watchit.entity.CommentLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentLikeMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "comment.id", target = "commentId")
    public CommentLikeResponseDto toDto(CommentLike commentLike);
    public List<CommentLikeResponseDto> toDtoList(List<CommentLike> commentLikes);
    public CommentLike toEntity(CommentLikeRequestDto commentLikeRequestDto);

}
