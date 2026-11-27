package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.repository.CommentLikeRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.yutkubeygo.watchit.dto.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.CommentMapper;
import com.yutkubeygo.watchit.repository.CommentRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final NotificationService notificationService;
    private final CommentLikeRepository commentLikeRepository;

    public CommentService(CommentRepository commentRepository, CommentMapper commentMapper, UserRepository userRepository, VideoRepository videoRepository, NotificationService notificationService, CommentLikeRepository commentLikeRepository) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.notificationService = notificationService;
    }

    //Yorum ve bildirimi tek transaction'da yazılır.
    //Cevapsa üst yorumun yazarına, değilse video sahibine bildirim gider.
    @Transactional
    public CommentResponseDto createComment(Long userId,CommentRequestDto commentRequestDto)
    {
        //Gelen json dosyasina gore yeni bir nesne olusturduk
        Comment comment = commentMapper.toEntity(commentRequestDto);

        //Kullanici ve kanal id bilgielri uzerinden o gerekli filedlara erisip revize ettik
        User user = userRepository.findById(userId).orElse(null);
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

        Comment saved = commentRepository.save(comment);

        if(saved.getParentComment()!=null)
            notificationService.notify(user, saved.getParentComment().getUser(), NotificationType.COMMENT,
                    "Yorumuna yanıt", user.getUsername() + " yorumuna yanıt verdi: " + saved.getContent());
        else
            notificationService.notify(user, video.getChannel().getOwner(), NotificationType.COMMENT,
                    "Yeni yorum", user.getUsername() + " \"" + video.getTitle() + "\" videona yorum yaptı: " + saved.getContent());

        //Bu nesneyi kaydettikten sonra dto nesneine cevirip return ettik
        return  commentMapper.toDto(saved);
    }

    public CommentResponseDto getCommentById(Long id)
    {
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return null;

        return commentMapper.toDto(comment);
    }


    //Yorum başkasınınsa ForbiddenException fırlatır (403)
    public CommentResponseDto updateComment(Long id, Long userId, CommentRequestDto commentRequestDto)
    {
        //db'de boyle bir comment var mi ?
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return null;

        //Yorumu yalnizca yazari degistirebilir
        Long ownerId = comment.getUser().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        //Düzenlemede yalnız metin değişir: yorumun videosu ve bağlı olduğu üst yorum sabit kalır
        //(eskiden istekteki videoId/parentCommentId ile yorum başka videoya taşınabiliyordu)
        comment.setContent(commentRequestDto.getContent());
        comment.setEdited(true);

        return commentMapper.toDto(commentRepository.save(comment));

    }

    //Videonun yorumları sayfa sayfa; video yoksa null (404).
    //sort: "new" (en yeni) ya da "top" (en beğenilen). Sabitlenmiş yorum ilk sayfanın en üstünde gelir, diğer sayfalarda yok.
    //Sayfalanan üst yorumlardır; o sayfadaki üst yorumların cevapları da (hepsi) aynı listede gelir, frontend gruplar.
    //Her yorumda beğeni sayısı ve (giriş yapmışsa) beğenip beğenmediğim. total: cevaplar dahil bütün yorumlar
    public PageResponseDto<CommentResponseDto> getCommentsByVideoId(Long videoId, String sort, Long userId, Pageable pageable)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null)
            return null;

        Long pinnedId = video.getPinnedCommentId();
        long exclude = pinnedId == null ? -1L : pinnedId;
        Page<Comment> topLevel = "top".equals(sort)
                ? commentRepository.findTopTopLevel(videoId, exclude, pageable)
                : commentRepository.findNewestTopLevel(videoId, exclude, pageable);

        List<Comment> tops = new ArrayList<>();
        if(pinnedId != null && pageable.getPageNumber() == 0)
            commentRepository.findById(pinnedId).ifPresent(tops::add);
        tops.addAll(topLevel.getContent());

        List<Comment> comments = new ArrayList<>(tops);
        if(!tops.isEmpty())
            comments.addAll(commentRepository.findByParentCommentIdInOrderByCreatedAtDesc(tops.stream().map(Comment::getId).toList()));

        List<CommentResponseDto> dtos = commentMapper.toDtoList(comments);
        fillLikesAndPin(dtos, userId, pinnedId);
        return new PageResponseDto<>(dtos, topLevel.getNumber(), topLevel.getSize(),
                topLevel.hasNext(), commentRepository.countByVideoId(videoId));
    }

    //Beğeni sayıları ve "beğendim mi" iki sorguda (yorum başına ayrı sorgu değil)
    private void fillLikesAndPin(List<CommentResponseDto> dtos, Long userId, Long pinnedId)
    {
        if(dtos.isEmpty())
            return;
        List<Long> ids = dtos.stream().map(CommentResponseDto::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for(Object[] row : commentLikeRepository.countByCommentIds(ids))
            counts.put((Long) row[0], (Long) row[1]);
        Set<Long> liked = userId == null ? Set.of() : new HashSet<>(commentLikeRepository.findLikedCommentIds(userId, ids));
        for(CommentResponseDto dto : dtos)
        {
            dto.setLikeCount(counts.getOrDefault(dto.getId(), 0L));
            dto.setLikedByMe(liked.contains(dto.getId()));
            dto.setPinned(dto.getId().equals(pinnedId));
        }
    }

    //Video sahibi bir üst yorumu sabitler (commentId null: sabiti kaldırır).
    //Video yoksa false (404), sahibi değilse 403, yorum bu videonun üst yorumu değilse 400
    public boolean pinComment(Long videoId, Long commentId, Long userId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null)
            return false;
        if(!video.getChannel().getOwner().getId().equals(userId))
            throw new ForbiddenException();
        if(commentId != null)
        {
            Comment comment = commentRepository.findById(commentId).orElse(null);
            if(comment == null || !comment.getVideo().getId().equals(videoId) || comment.getParentComment() != null)
                throw new BadRequestException();
        }
        videoRepository.setPinnedComment(videoId, commentId);
        return true;
    }


    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Yorum başkasınınsa ForbiddenException fırlatır (403)
    public boolean deleteComment(Long id, Long userId)
    {
        Comment comment = commentRepository.findById(id).orElse(null);
        if(comment==null)
            return false;

        //Yorumu yazarı ya da videonun sahibi silebilir (YouTube'daki gibi: kanal sahibi kendi videosundaki yorumları yönetir)
        Long authorId = comment.getUser().getId();
        Long videoOwnerId = comment.getVideo().getChannel().getOwner().getId();
        if(!authorId.equals(userId) && !videoOwnerId.equals(userId))
            throw new ForbiddenException();

        commentRepository.delete(comment);
        return true;
    }

    //Yönetici (şikayet üzerine) sahiplik kontrolü olmadan siler (cevapları ve beğenileri de gider); yorum yoksa false
    public boolean deleteCommentAsAdmin(Long id)
    {
        if(!commentRepository.existsById(id))
            return false;
        commentRepository.deleteCommentById(id);
        return true;
    }

}
