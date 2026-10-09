package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.WatchHistory;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.WatchHistoryMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.WatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;


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



    public WatchHistoryResponseDto createOrUpdateWatchHistory(WatchHistoryRequestDto watchHistoryRequestDto,Long userId)
    {

        WatchHistory watchHistory = watchHistoryRepository.findByUserIdAndVideoId(userId,watchHistoryRequestDto.getVideoId()).orElse(null);
        if(watchHistory == null)
        {

            //Kullanıcı ya da video yoksa diğer servislerdeki gibi null dönüyoruz (controller 404 verir)
            User user = userRepository.findById(userId).orElse(null);
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


    public WatchHistoryResponseDto getWatchHistoryById(Long id, Long userId)
    {
        WatchHistory watchHistory = watchHistoryRepository.findById(id).orElse(null);
        if(watchHistory==null)
            return null;

        if(!watchHistory.getUser().getId().equals(userId))
            throw new ForbiddenException();


        return watchHistoryMapper.toDto(watchHistory);
    }



    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Kayıt başkasınınsa ForbiddenException fırlatır (403)
    public boolean deleteWatchHistoryById(Long id, Long userId)
    {
        WatchHistory watchHistory = watchHistoryRepository.findById(id).orElse(null);
        if(watchHistory==null)
            return false;

        Long ownerId = watchHistory.getUser().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        watchHistoryRepository.deleteById(id);
        return true;
    }

    public List<WatchHistoryResponseDto> getWatchHistoryByUserId(Long userId) {
        List<WatchHistory> userHistory = watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId);


        return watchHistoryMapper.toDtoList(userHistory);
    }

}
