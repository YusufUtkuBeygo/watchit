package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.WatchLater;
import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.WatchLaterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

//"Daha sonra izle": listeyi yalnız sahibi görür, kullanıcı hep token'dan gelir
@Service
public class WatchLaterService {

    public enum AddResult { ADDED, ALREADY_SAVED, VIDEO_NOT_FOUND }

    private final WatchLaterRepository watchLaterRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoMapper videoMapper;

    public WatchLaterService(WatchLaterRepository watchLaterRepository, VideoRepository videoRepository,
                             UserRepository userRepository, VideoMapper videoMapper)
    {
        this.watchLaterRepository = watchLaterRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.videoMapper = videoMapper;
    }

    public PageResponseDto<VideoResponseDto> getMyList(Long userId, Pageable pageable)
    {
        Page<WatchLater> page = watchLaterRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponseDto.of(page, videoMapper.toDtoList(page.map(WatchLater::getVideo).getContent()));
    }

    //Zaten listedeyse hata değil: düğmeye iki kez basılması sorun çıkarmasın
    public AddResult add(Long userId, Long videoId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null)
            return AddResult.VIDEO_NOT_FOUND;
        if(watchLaterRepository.existsByUserIdAndVideoId(userId, videoId))
            return AddResult.ALREADY_SAVED;

        User user = userRepository.getReferenceById(userId);
        WatchLater item = new WatchLater();
        item.setUser(user);
        item.setVideo(video);
        watchLaterRepository.save(item);
        return AddResult.ADDED;
    }

    //Listede yoksa false (controller 404 döner)
    public boolean remove(Long userId, Long videoId)
    {
        WatchLater item = watchLaterRepository.findByUserIdAndVideoId(userId, videoId).orElse(null);
        if(item == null)
            return false;
        watchLaterRepository.delete(item);
        return true;
    }

    public boolean isSaved(Long userId, Long videoId)
    {
        return userId != null && watchLaterRepository.existsByUserIdAndVideoId(userId, videoId);
    }
}
