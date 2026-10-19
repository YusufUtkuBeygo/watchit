package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    //Requestten gelen bilgileri dto nesnesine dönüştürür
    //Request DTO'sunda olmayan alanlar (id, ilişkiler, tarihler) serviste ya da veritabanında dolar
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Video toEntity(VideoRequestDto videoRequestDto);
    //İşlem sonucu return edilecek nesneyi RepsonseDto nesnesine dönüştürür
    @Mapping(source = "channel.id", target="channelId")
    @Mapping(source = "category.id", target="categoryId")
    @Mapping(source = "channel.channelName", target="channelName")
    @Mapping(source = "channel.profilePictureUrl", target="channelProfilePictureUrl")
    //Beğeni sayısı ve "beğendim mi" tabloda yok, servis doldurur
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "likedByMe", ignore = true)
    VideoResponseDto toDto(Video video);
    //Liste halinde isteklerin returnleri için ResponseDto nesnelerinden oluşan bir liste return eder
    List<VideoResponseDto> toDtoList(List<Video>  videoResponseDtoList);

}
