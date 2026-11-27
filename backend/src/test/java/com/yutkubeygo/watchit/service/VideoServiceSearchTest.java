package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

//VideoService.searchVideos: boş arama veritabanına gitmez; metin kırpılıp LIKE jokerleri kaçırılarak repository'ye verilir.
@ExtendWith(MockitoExtension.class)
class VideoServiceSearchTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoService videoService;

    private static final Pageable PAGE = PageRequest.of(0, 24);

    @Test
    void blankSearchReturnsEmptyWithoutQuery()
    {
        assertThat(videoService.searchVideos("   ", PAGE).items()).isEmpty();
        assertThat(videoService.searchVideos("", PAGE).hasNext()).isFalse();
        verify(videoRepository, never()).search(anyString(), any(Pageable.class));
    }

    @Test
    void searchTextIsTrimmedAndEscaped()
    {
        when(videoRepository.search(anyString(), any(Pageable.class))).thenReturn(Page.empty(PAGE));

        videoService.searchVideos("  %100_kod  ", PAGE);

        verify(videoRepository).search(eq("\\%100\\_kod"), eq(PAGE));
    }

    @Test
    void escapeLikeEscapesWildcardsAndBackslash()
    {
        assertThat(VideoService.escapeLike("kod")).isEqualTo("kod");
        assertThat(VideoService.escapeLike("%")).isEqualTo("\\%");
        assertThat(VideoService.escapeLike("a_b")).isEqualTo("a\\_b");
        //\ önce kaçırılır: "\%" -> "\\" + "\%"
        assertThat(VideoService.escapeLike("\\%")).isEqualTo("\\\\\\%");
    }
}
