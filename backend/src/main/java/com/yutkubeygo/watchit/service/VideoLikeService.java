package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoLike;
import com.yutkubeygo.watchit.mapper.VideoLikeMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoLikeRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoLikeService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoLikeRepository videoLikeRepository;

    public VideoLikeService(UserRepository userRepository, VideoRepository videoRepository, VideoLikeMapper videoLikeMapper, VideoLikeRepository videoLikeRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.videoLikeMapper = videoLikeMapper;
        this.videoLikeRepository = videoLikeRepository;
    }

    public VideoLikeResponseDto createVideoLike(VideoLikeRequestDto videoLikeRequestDto)
    {
        User user = userRepository.findById(videoLikeRequestDto.getUserId()).orElse(null);
        if(user==null)
            return null;

        Video video = videoRepository.findById(videoLikeRequestDto.getVideoId()).orElse(null);
        if(video==null)
            return null;

        VideoLike videoLike = videoLikeMapper.toEntity(videoLikeRequestDto);

        videoLike.setUser(user);
        videoLike.setVideo(video);

        return videoLikeMapper.toDto(videoLikeRepository.save(videoLike));
    }

    public VideoLikeResponseDto getVideoLikeById(Long id)
    {
        VideoLike videoLike = videoLikeRepository.findById(id).orElse(null);
        if(videoLike==null)
            return null;
        return videoLikeMapper.toDto(videoLike);
    }

    public List<VideoLikeResponseDto> getAllVideoLikes()
    {
        return videoLikeMapper.toDtoList(videoLikeRepository.findAll());
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteVideoLikeById(Long id)
    {
        if(!videoLikeRepository.existsById(id))
            return false;

        videoLikeRepository.deleteById(id);
        return true;
    }

    //Update methodu eklemdik burda update edecek herhanigi bir ozellik yok sadece vide ile begenisi arasinda iliski kuran basit bir entity

}
