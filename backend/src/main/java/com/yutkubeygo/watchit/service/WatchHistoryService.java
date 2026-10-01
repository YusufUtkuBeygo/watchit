package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.WatchHistory;
import com.yutkubeygo.watchit.mapper.WatchHistoryMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.WatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final WatchHistoryMapper watchHistoryMapper;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;


    public WatchHistoryService(WatchHistoryRepository watchHistoryRepository, WatchHistoryMapper watchHistoryMapper, UserRepository userRepository, VideoRepository videoRepository) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.watchHistoryMapper = watchHistoryMapper;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }



    public WatchHistoryResponseDto createOrUpdateWatchHistory(WatchHistoryRequestDto watchHistoryRequestDto)
    {

        WatchHistory watchHistory = watchHistoryRepository.findByUserIdAndVideoId(watchHistoryRequestDto.getUserId(),watchHistoryRequestDto.getVideoId()).orElse(null);
        if(watchHistory == null)
        {

            //Kullanıcı ya da video yoksa diğer servislerdeki gibi null dönüyoruz (controller 404 verir)
            User user = userRepository.findById(watchHistoryRequestDto.getUserId()).orElse(null);
            if(user == null)
                return null;

            Video video = videoRepository.findById(watchHistoryRequestDto.getVideoId()).orElse(null);
            if(video == null)
                return null;

            WatchHistory watchHistoryEntity = watchHistoryMapper.toEntity(watchHistoryRequestDto);

            watchHistoryEntity.setUser(user);
            watchHistoryEntity.setVideo(video);

            return watchHistoryMapper.toDto(watchHistoryRepository.save(watchHistoryEntity));
        }

        else {

            watchHistory.setWatchedSeconds(watchHistoryRequestDto.getWatchedSeconds());
            watchHistory.setIsCompleted(watchHistoryRequestDto.getIsCompleted());
            return watchHistoryMapper.toDto(watchHistoryRepository.save(watchHistory));
        }

    }


    public WatchHistoryResponseDto getWatchHistoryById(Long id)
    {
        WatchHistory watchHistory = watchHistoryRepository.findById(id).orElse(null);
        if(watchHistory==null)
            return null;
        else
            return watchHistoryMapper.toDto(watchHistory);
    }

    public List<WatchHistoryResponseDto> getAllWatchHistory()
    {
        return watchHistoryMapper.toDtoList(watchHistoryRepository.findAll());
    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteWatchHistoryById(Long id)
    {
        if(!watchHistoryRepository.existsById(id))
            return false;

        watchHistoryRepository.deleteById(id);
        return true;
    }

    public List<WatchHistoryResponseDto> getWatchHistoryByUserId(Long userId) {
        List<WatchHistory> userHistory = watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId);
        return watchHistoryMapper.toDtoList(userHistory);
    }

}
