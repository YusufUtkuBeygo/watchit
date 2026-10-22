package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//VideoService.recordView: video yoksa false; görevli "say" derse sayı artar, demezse artmaz ama yine true döner.
//Veritabanı yerine sahte (mock) repository ve görevli kullanılır.
@ExtendWith(MockitoExtension.class)
class VideoServiceRecordViewTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ViewLimiterService viewLimiterService;

    @InjectMocks
    private VideoService videoService;

    private Video videoWithDuration(int seconds)
    {
        Video video = new Video();
        video.setId(1L);
        video.setDurationInSeconds(seconds);
        return video;
    }

    @Test
    void missingVideoReturnsFalseAndCountsNothing()
    {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(videoService.recordView(99L, null, "1.2.3.4")).isFalse();
        verify(viewLimiterService, never()).tryCount(anyString(), anyLong(), any());
        verify(videoRepository, never()).incrementViewCount(anyLong());
    }

    @Test
    void countedViewIncrements()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(videoWithDuration(10)));
        when(viewLimiterService.tryCount("userId:5", 1L, 10)).thenReturn(true);

        assertThat(videoService.recordView(1L, 5L, "1.2.3.4")).isTrue();
        verify(videoRepository).incrementViewCount(1L);
    }

    @Test
    void limitedViewDoesNotIncrementButStillReturnsTrue()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(videoWithDuration(10)));
        when(viewLimiterService.tryCount("userId:5", 1L, 10)).thenReturn(false);

        assertThat(videoService.recordView(1L, 5L, "1.2.3.4")).isTrue();
        verify(videoRepository, never()).incrementViewCount(anyLong());
    }

    @Test
    void anonymousViewerIsIdentifiedByIp()
    {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(videoWithDuration(10)));
        when(viewLimiterService.tryCount("ip:1.2.3.4", 1L, 10)).thenReturn(true);

        videoService.recordView(1L, null, "1.2.3.4");

        verify(viewLimiterService).tryCount("ip:1.2.3.4", 1L, 10);
        verify(videoRepository).incrementViewCount(1L);
    }
}
