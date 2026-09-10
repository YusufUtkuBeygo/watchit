package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.CommentLikeRequestDto;
import com.yutkubeygo.watchit.dto.CommentLikeResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.entity.CommentLike;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.mapper.CommentLikeMapper;
import com.yutkubeygo.watchit.repository.CommentLikeRepository;
import com.yutkubeygo.watchit.repository.CommentRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentLikeService {


    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentLikeMapper commentLikeMapper;

    public CommentLikeService(UserRepository userRepository, CommentRepository commentRepository, CommentLikeRepository commentLikeRepository, CommentLikeMapper commentLikeMapper) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.commentLikeMapper = commentLikeMapper;
    }

    public CommentLikeResponseDto createCommentLike(CommentLikeRequestDto commentLikeRequestDto)
    {
        User user = userRepository.findById(commentLikeRequestDto.getUserId()).orElse(null);
        if(user==null)
            return null;

        Comment comment = commentRepository.findById(commentLikeRequestDto.getCommentId()).orElse(null);
        if(comment==null)
            return null;

        CommentLike commentLike = commentLikeMapper.toEntity(commentLikeRequestDto);

        commentLike.setComment(comment);
        commentLike.setUser(user);
        return commentLikeMapper.toDto(commentLikeRepository.save(commentLike));

    }

    public CommentLikeResponseDto getCommentLikeById(Long id)
    {
        CommentLike commentLike = commentLikeRepository.findById(id).orElse(null);
        if(commentLike==null)
            return null;

        return commentLikeMapper.toDto(commentLike);
    }

    public List<CommentLikeResponseDto> getAllCommentLikes()
    {
        return commentLikeMapper.toDtoList(commentLikeRepository.findAll());
    }

    public void deleteCommentLike (Long id)
    {
        commentLikeRepository.deleteById(id);
    }

}
