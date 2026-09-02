package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.WatchHistoryRequestDto;
import com.yutkubeygo.watchit.dto.WatchHistoryResponseDto;
import com.yutkubeygo.watchit.entity.WatchHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel="spring")
public interface WatchHistoryMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "video.id", target = "videoId")
    public WatchHistoryResponseDto toDto(WatchHistory history);
    public List<WatchHistoryResponseDto> toDtoList(List<WatchHistory> historyList);
    public WatchHistory toEntity(WatchHistoryRequestDto watchHistoryRequestDto);

}
