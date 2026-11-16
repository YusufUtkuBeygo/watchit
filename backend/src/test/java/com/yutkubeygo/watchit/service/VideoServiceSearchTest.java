package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void blankSearchReturnsEmptyWithoutQuery()
    {
        assertThat(videoService.searchVideos("   ")).isEmpty();
        assertThat(videoService.searchVideos("")).isEmpty();
        verify(videoRepository, never()).searchByTitle(anyString());
    }

    @Test
    void searchTextIsTrimmedAndEscaped()
    {
        videoService.searchVideos("  %100_kod  ");

        verify(videoRepository).searchByTitle("\\%100\\_kod");
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
