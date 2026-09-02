package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.Video;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    //Requestten gelen bilgileri dto nesnesine dönüştürür
    Video toEntity(VideoRequestDto videoRequestDto);
    //İşlem sonucu return edilecek nesneyi RepsonseDto nesnesine dönüştürür
    VideoResponseDto toDto(Video video);
    //Liste halinde isteklerin returnleri için ResponseDto nesnelerinden oluşan bir liste return eder
    List<VideoResponseDto> toDtoList(List<Video>  videoResponseDtoList);

}
