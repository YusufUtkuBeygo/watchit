package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoService {

    private final VideoMapper videoMapper;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;

    public VideoService(VideoMapper videoMapper, ChannelRepository channelRepository, CategoryRepository categoryRepository, VideoRepository videoRepository) {
        this.videoMapper = videoMapper;
        this.channelRepository = channelRepository;
        this.categoryRepository = categoryRepository;
        this.videoRepository = videoRepository;
    }

    public VideoResponseDto getVideo(Long id)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return null;
        return videoMapper.toDto(video);
    }

    //VideoMapperdaki list methoduna dikkat et hata cikarsa return
    public List<VideoResponseDto> getAllVideos()
    {
        List<Video> videos=videoRepository.findAll();
        return videoMapper.toDtoList(videos);
    }

    public VideoResponseDto createVideo(VideoRequestDto videoRequestDto,Long userId)
    {
        Video video=videoMapper.toEntity(videoRequestDto);

        Channel channel= channelRepository.findById(videoRequestDto.getChannelId()).orElse(null);

        if(channel == null)
        {
            return null;
        }

        Long ownerId = channel.getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);

        if(category==null)
        {
            return null;
        }

        video.setChannel(channel);
        video.setCategory(category);

        Video savedVideo= videoRepository.save(video);
        return videoMapper.toDto(savedVideo);

    }

    //Video başkasının kanalındaysa ya da başkasının kanalına taşınmak isteniyorsa ForbiddenException fırlatır (403)
    public VideoResponseDto updateVideo(Long id,VideoRequestDto videoRequestDto,Long userId)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return null;

        Long ownerId=video.getChannel().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);
        if(category==null)
            return null;

        Channel channel = channelRepository.findById(videoRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;

        //Videonun taşınacağı kanal da kullanıcının olmalı
        Long newOwnerId=channel.getOwner().getId();
        if(!newOwnerId.equals(userId))
            throw new ForbiddenException();

        //json`dan gelen degistirilecek veriler burda set edilir
        video.setChannel(channel);
        video.setCategory(category);
        video.setTitle(videoRequestDto.getTitle());
        video.setDescription((videoRequestDto.getDescription()));
        video.setThumbnailUrl(videoRequestDto.getThumbnailUrl());
        video.setVideoUrl(videoRequestDto.getVideoUrl());
        video.setDurationInSeconds(videoRequestDto.getDurationInSeconds());

        Video savedVideo=videoRepository.save(video);

        return videoMapper.toDto(savedVideo);

    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Video başkasının kanalındaysa ForbiddenException fırlatır (403)
    public boolean deleteVideo(Long id, Long userId)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return false;

        //Eğer kullanıcı varsa ama sileceği video ile ilgili yetkisi yoksa exception fırlatır
        Long ownerId=video.getChannel().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        videoRepository.deleteById(id);
        return true;
    }



}
