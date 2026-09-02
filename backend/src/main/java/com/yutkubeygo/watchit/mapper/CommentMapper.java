package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentResponseDto toDto(Comment comment);
    List<CommentResponseDto> toDtoList(List<Comment> comments);
    Comment toEntity(CommentRequestDto commentRequestDto);

}
