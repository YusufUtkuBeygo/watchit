package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Video;
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

    public VideoResponseDto createVideo(VideoRequestDto videoRequestDto)
    {
        Video video=videoMapper.toEntity(videoRequestDto);

        Channel channel= channelRepository.findById(videoRequestDto.getChannelId()).orElse(null);


        if(channel == null)
        {
            System.out.println("CHANNEL YOK: " + videoRequestDto.getChannelId());
            return null;
        }

        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);

        if(category==null)
        {
            System.out.println("CATEGORY YOK: " + videoRequestDto.getCategoryId());
            return null;
        }

        video.setChannel(channel);
        video.setCategory(category);

        Video savedVideo= videoRepository.save(video);
        return videoMapper.toDto(savedVideo);

    }

    public VideoResponseDto updateVideo(Long id,VideoRequestDto videoRequestDto)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return null;


        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);
        if(category==null)
            return null;

        Channel channel = channelRepository.findById(videoRequestDto.getChannelId()).orElse(null);
        if(channel==null)
            return null;

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

    public void deleteVideo(Long id)
    {
        videoRepository.deleteById(id);
    }



}
