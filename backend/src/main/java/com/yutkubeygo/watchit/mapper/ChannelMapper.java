package com.yutkubeygo.watchit.mapper;
import com.yutkubeygo.watchit.dto.ChannelRequestDto;
import com.yutkubeygo.watchit.dto.ChannelResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import org.mapstruct.Mapper;

import java.util.List;

//Nesneyi dto nesnesine çevirme işini otomatik yapmak için kullanılıyor
@Mapper(componentModel = "spring")
public interface ChannelMapper {
    //Gönderilecek nesneyi dto'ya çeviri
    ChannelResponseDto toDto(Channel channel);
    //Gönderilecek nesne listesini dto listesine çevirir
    List<ChannelResponseDto> toDtoList(List<Channel> channels);
    //Json dosyasından gelen bilgieri istenilen dto nesnesindeki kısıtlamaları uygularyak dto obj. oluşturur
    Channel toEntity(ChannelRequestDto dto);
}
