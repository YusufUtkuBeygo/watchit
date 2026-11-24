package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.CourseResponseDto;
import com.yutkubeygo.watchit.entity.Playlist;
import com.yutkubeygo.watchit.entity.PlaylistVideo;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.PlaylistRepository;
import com.yutkubeygo.watchit.repository.PlaylistVideoRepository;
import com.yutkubeygo.watchit.repository.WatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

//Kurslar: "kurs" işaretli herkese açık oynatma listeleri. Ayrı tablo yok; kurs = sıralı bir liste.
//İlerleme, izleme geçmişinden hesaplanır (o kursun videolarından kaçında kaydım var).
@Service
public class CourseService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    public CourseService(PlaylistRepository playlistRepository, PlaylistVideoRepository playlistVideoRepository,
                         WatchHistoryRepository watchHistoryRepository)
    {
        this.playlistRepository = playlistRepository;
        this.playlistVideoRepository = playlistVideoRepository;
        this.watchHistoryRepository = watchHistoryRepository;
    }

    //Boş kurslar (henüz videosu olmayan) gösterilmez: tıklayınca açılacak ders yok
    public List<CourseResponseDto> getCourses(Long userId)
    {
        return playlistRepository.findByIsCourseTrueAndIsPublicTrueOrderByUpdatedAtDescIdDesc().stream()
                .map(playlist -> toDto(playlist, userId))
                .filter(course -> course.videoCount() > 0)
                .toList();
    }

    private CourseResponseDto toDto(Playlist playlist, Long userId)
    {
        //Dersler liste sırasıyla; ilk ders "Kursa başla" düğmesinin gideceği video
        List<PlaylistVideo> lessons = playlistVideoRepository.findByPlaylistIdOrderByOrderIndexAscIdAsc(playlist.getId());
        List<Long> videoIds = lessons.stream().map(PlaylistVideo::getVideo).map(Video::getId).toList();
        Video first = lessons.isEmpty() ? null : lessons.getFirst().getVideo();
        //Girişsizse ilerleme sorulmaz
        long watched = (userId == null || videoIds.isEmpty()) ? 0 : watchHistoryRepository.countByUserIdAndVideoIdIn(userId, videoIds);

        return new CourseResponseDto(playlist.getId(), playlist.getTitle(), playlist.getDescription(),
                playlist.getOwner().getUsername(), videoIds.size(), watched,
                first == null ? null : first.getId(), first == null ? null : first.getThumbnailUrl(),
                playlist.getUpdatedAt());
    }
}
