package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ClipRequestDto;
import com.yutkubeygo.watchit.dto.ClipResponseDto;
import com.yutkubeygo.watchit.entity.Clip;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.ClipRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

//Klipler: herkes izlediği bir videodan 5-60 saniyelik parça kesip kaydedebilir.
//Klibi bağlantısı olan herkes izler (YouTube gibi); listeyi ve silmeyi yalnız sahibi görür/yapar.
@Service
public class ClipService {

    public static final int MIN_LENGTH = 5;
    public static final int MAX_LENGTH = 60;

    private final ClipRepository clipRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoMapper videoMapper;

    public ClipService(ClipRepository clipRepository, VideoRepository videoRepository,
                       UserRepository userRepository, VideoMapper videoMapper)
    {
        this.clipRepository = clipRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.videoMapper = videoMapper;
    }

    //Video yoksa null (controller 404), aralık kurala uymuyorsa BadRequestException (400)
    public ClipResponseDto createClip(Long userId, ClipRequestDto request)
    {
        Video video = videoRepository.findById(request.getVideoId()).orElse(null);
        if(video == null)
            return null;

        int start = request.getStartSeconds();
        int end = request.getEndSeconds();
        int length = end - start;
        if(length < MIN_LENGTH || length > MAX_LENGTH)
            throw new BadRequestException();
        //Süre biliniyorsa klip videonun dışına taşamaz (1 sn pay: tarayıcı süreyi yuvarlıyor)
        Integer duration = video.getDurationInSeconds();
        if(duration != null && duration > 0 && end > duration + 1)
            throw new BadRequestException();

        Clip clip = new Clip();
        clip.setUser(userRepository.getReferenceById(userId));
        clip.setVideo(video);
        clip.setTitle(request.getTitle().trim());
        clip.setStartSeconds(start);
        clip.setEndSeconds(end);
        return toDto(clipRepository.save(clip));
    }

    public List<ClipResponseDto> getMyClips(Long userId)
    {
        return clipRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(this::toDto).toList();
    }

    //Tek klip herkese açık: bağlantıyı paylaşmanın anlamı bu
    public ClipResponseDto getClip(Long id)
    {
        return clipRepository.findById(id).map(this::toDto).orElse(null);
    }

    //Yoksa false (404), başkasınınsa ForbiddenException (403)
    public boolean deleteClip(Long id, Long userId)
    {
        Clip clip = clipRepository.findById(id).orElse(null);
        if(clip == null)
            return false;
        if(!clip.getUser().getId().equals(userId))
            throw new ForbiddenException();
        clipRepository.delete(clip);
        return true;
    }

    private ClipResponseDto toDto(Clip clip)
    {
        return new ClipResponseDto(clip.getId(), clip.getTitle(), clip.getStartSeconds(), clip.getEndSeconds(),
                clip.getCreatedAt(), clip.getUser().getId(), clip.getUser().getUsername(),
                videoMapper.toDto(clip.getVideo()));
    }
}
