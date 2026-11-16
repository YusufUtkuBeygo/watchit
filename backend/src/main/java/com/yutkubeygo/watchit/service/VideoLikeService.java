package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoLike;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.VideoLikeMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoLikeRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideoLikeService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoLikeRepository videoLikeRepository;
    private final NotificationService notificationService;

    public VideoLikeService(UserRepository userRepository, VideoRepository videoRepository, VideoLikeMapper videoLikeMapper, VideoLikeRepository videoLikeRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.videoLikeMapper = videoLikeMapper;
        this.videoLikeRepository = videoLikeRepository;
        this.notificationService = notificationService;
    }

    //Beğeni ve video sahibine giden bildirim tek transaction'da yazılır
    //(çift beğeni 409 alırsa bildirim de yazılmaz)
    @Transactional
    public VideoLikeResponseDto createVideoLike(VideoLikeRequestDto videoLikeRequestDto,Long userId)
    {
        User user = userRepository.findById(userId).orElse(null);
        if(user==null)
            return null;

        Video video = videoRepository.findById(videoLikeRequestDto.getVideoId()).orElse(null);
        if(video==null)
            return null;

        VideoLike videoLike = videoLikeMapper.toEntity(videoLikeRequestDto);

        videoLike.setUser(user);
        videoLike.setVideo(video);

        VideoLike saved = videoLikeRepository.save(videoLike);

        notificationService.notify(user, video.getChannel().getOwner(), NotificationType.LIKE,
                "Videon beğenildi", user.getUsername() + " \"" + video.getTitle() + "\" videonu beğendi.");

        return videoLikeMapper.toDto(saved);
    }

    //Beğeni yoksa null (404), başkasınınsa ForbiddenException (403)
    public VideoLikeResponseDto getVideoLikeById(Long id, Long userId)
    {
        VideoLike videoLike = videoLikeRepository.findById(id).orElse(null);
        if(videoLike==null)
            return null;

        if(!videoLike.getUser().getId().equals(userId))
            throw new ForbiddenException();

        return videoLikeMapper.toDto(videoLike);
    }

    //Yalnız kendi beğenilerim: sorgu baştan kullanıcının id'siyle yapılır
    public List<VideoLikeResponseDto> getMyVideoLikes(Long userId)
    {
        return videoLikeMapper.toDtoList(videoLikeRepository.findByUserId(userId));
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteMyVideoLike(Long videoId, Long userId)
    {
        VideoLike myLike = videoLikeRepository.findByUserIdAndVideoId(userId,videoId).orElse(null);
        if(myLike==null)
            return false;

        videoLikeRepository.delete(myLike);
        return true;
    }

    //Update methodu eklemdik burda update edecek herhanigi bir ozellik yok sadece vide ile begenisi arasinda iliski kuran basit bir entity

}
