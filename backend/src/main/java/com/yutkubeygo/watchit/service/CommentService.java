package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.mapper.CommentMapper;
import com.yutkubeygo.watchit.repository.CommentRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public CommentService(CommentRepository commentRepository, CommentMapper commentMapper, UserRepository userRepository, VideoRepository videoRepository) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    public CommentResponseDto createComment(CommentRequestDto commentRequestDto)
    {
        //Gelen json dosyasina gore yeni bir nesne olusturduk
        Comment comment = commentMapper.toEntity(commentRequestDto);

        //Kullanici ve kanal id bilgielri uzerinden o gerekli filedlara erisip revize ettik
        User user = userRepository.findById(commentRequestDto.getUserId()).orElse(null);
        if(user==null)
            return null;

        Video video = videoRepository.findById(commentRequestDto.getVideoId()).orElse(null);

        if(video==null)
            return null;

        if(commentRequestDto.getParentCommentId()!=null)
        {
            Comment parentComment = commentRepository.findById(commentRequestDto.getParentCommentId()).orElse(null);
            if(parentComment==null)
                return null;

            //Cevap, ust yorumla ayni videoya yazilmis olmali
            if(!parentComment.getVideo().getId().equals(video.getId()))
                return null;

            comment.setParentComment(parentComment);

        }



        comment.setUser(user);
        comment.setVideo(video);

        //Bu nesneyi kaydettikten sonra dto nesneine cevirip return ettik
        return  commentMapper.toDto(commentRepository.save(comment));
    }

    public CommentResponseDto getCommentById(Long id)
    {
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return null;

        return commentMapper.toDto(comment);
    }

    public List<CommentResponseDto> getAllComments()
    {
        return commentMapper.toDtoList(commentRepository.findAll());
    }


    public CommentResponseDto updateComment(Long id, CommentRequestDto commentRequestDto)
    {
        //db'de boyle bir comment var mi ?
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return null;

        //db'de boyle bir user var mi ?
        User user = userRepository.findById(commentRequestDto.getUserId()).orElse(null);
        if(user==null)
            return null;

        //db'de boyle bir video var mi ?
        Video video = videoRepository.findById(commentRequestDto.getVideoId()).orElse(null);
        if(video==null)
            return null;

        //ilk basta parentComment alani null olarak ayarlanir gelen doysadaki bilgiere gore tekrar revize edilir
        Comment parentComment = null;

        if (commentRequestDto.getParentCommentId() != null) {
            parentComment = commentRepository
                    .findById(commentRequestDto.getParentCommentId())
                    .orElse(null);

            if(parentComment == null)
            return null;
        }


        comment.setContent(commentRequestDto.getContent());
        comment.setVideo(video);
        comment.setUser(user);
        comment.setParentComment(parentComment);
        comment.setEdited(true);

        return commentMapper.toDto(commentRepository.save(comment));

    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteComment(Long id)
    {
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return false;

        commentRepository.delete(comment);
        return true;
    }

}
