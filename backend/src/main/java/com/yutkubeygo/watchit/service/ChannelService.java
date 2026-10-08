package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ChannelRequestDto;
import com.yutkubeygo.watchit.dto.ChannelResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.mapper.ChannelMapper;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelService {

    private final ChannelMapper channelMapper;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    public ChannelService(ChannelMapper channelMapper, ChannelRepository channelRepository, UserRepository userRepository) {
        this.channelMapper = channelMapper;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
    }

    public ChannelResponseDto createChannel(ChannelRequestDto request,Long userId)
    {
        //Dışardan gelen json istek dosyasındaki bilgieri kullanarak bir nesne oluşturur
        Channel channel=channelMapper.toEntity(request);
        //Kanalın sahibi isteği atan kullanıcıdır, id'si token'dan gelir
        User owner= userRepository.findById(userId).orElse(null);
        if(owner==null)
            return null;
        channel.setHandle(
                request.getChannelName()
                        .toLowerCase()
                        .replace(" ", "-"));
        channel.setOwner(owner);
        //Bilgi aktarımı tamamlanıp nesne oluşturulduktan sonra db'ye kaydediliyor
        Channel savedChannel= channelRepository.save(channel);
        //dto nesnesine çevirip return ediyoruz
        return channelMapper.toDto(savedChannel);

    }

    public List<ChannelResponseDto> getAllChannels()
    {
        //Tüm kanalların içinde bulunduğu bir liste oluşturuldu
        List<Channel> channels=channelRepository.findAll();
        //Sonra bu liste dto nesneleri olacak şekilde revize edilerek yeni bir liste döndürülür
        return channelMapper.toDtoList(channels);
    }


    public ChannelResponseDto getChannelById(Long id)
    {
        //İstenilen ıd'deki videoyu repository aracılığıyla buluryoruz (YOKSA NULL)
        Channel channel=channelRepository.findById(id).orElse(null);
        if(channel==null)
            return null;
        //Sonra bulduğumuz/istenilen bu kanal nesnesini dto nesnesine çevirip return ediyoruz
        return channelMapper.toDto(channel);
    }

    public ChannelResponseDto updateChannel(Long id, ChannelRequestDto request)
    {
        //Değiştirilme/update edilmek istenilen nesneyi bukuyoru varsa atanır yoksa null
        Channel channel=channelRepository.findById(id).orElse(null);

        if(channel==null)
            return null;

        //Uptade istenilen elemanın bilgierini requestten gelen bilgieri kullanarak revize ediyoruz
        channel.setChannelName(request.getChannelName());
        channel.setBannerImageUrl(request.getBannerImageUrl());
        channel.setProfilePictureUrl(request.getProfilePictureUrl());
        channel.setDescription(request.getDescription());

        //Değişiklikleri kaydediyoruz
        Channel savedChannel=channelRepository.save(channel);
        //İşlem sonunda dto nesnesini return ediyoruz
        return channelMapper.toDto(savedChannel);

    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteChannelById(Long id)
    {
        if(!channelRepository.existsById(id))
            return false;

        channelRepository.deleteById(id);
        return true;
    }
}
