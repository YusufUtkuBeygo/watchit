package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ChannelRequestDto;
import com.yutkubeygo.watchit.dto.ChannelResponseDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.ChannelMapper;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.SubscriptionRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChannelService {

    private final ChannelMapper channelMapper;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FileService fileService;
    private final VideoRepository videoRepository;

    public ChannelService(ChannelMapper channelMapper, ChannelRepository channelRepository, UserRepository userRepository, SubscriptionRepository subscriptionRepository, FileService fileService, VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
        this.channelMapper = channelMapper;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.fileService = fileService;
    }

    public ChannelResponseDto createChannel(ChannelRequestDto request,Long userId)
    {
        //Dışardan gelen json istek dosyasındaki bilgieri kullanarak bir nesne oluşturur
        checkFileUrls(request);

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


    public ChannelResponseDto getChannelById(Long id,Long userId)
    {
        //İstenilen ıd'deki videoyu repository aracılığıyla buluryoruz (YOKSA NULL)
        Channel channel=channelRepository.findById(id).orElse(null);
        if(channel==null)
            return null;
        //Sonra bulduğumuz/istenilen bu kanal nesnesini dto nesnesine çevirip return ediyoruz
        ChannelResponseDto dto=toDtoWithCounts(channel);
        if(userId!=null)
        {
            Optional<Subscription> mySubscription = subscriptionRepository.findBySubscriberIdAndChannelId(userId,id);
            if(mySubscription.isPresent())
                dto.setSubscribedByMe(true);

        }
        return dto;
    }

    //Kullanıcının tek kanalını token'daki id ile bulur, kanalı yoksa null
    public ChannelResponseDto getMyChannel(Long userId)
    {
        Channel channel=channelRepository.findByOwnerId(userId).orElse(null);
        if(channel==null)
            return null;
        return toDtoWithCounts(channel);
    }

    //Kanalı dto'ya çevirir, abone ve video sayısını sayarak doldurur (tabloda sayaç sütunu yok, V7)
    private ChannelResponseDto toDtoWithCounts(Channel channel)
    {
        ChannelResponseDto dto=channelMapper.toDto(channel);
        dto.setSubscriberCount(subscriptionRepository.countByChannelId(channel.getId()));
        dto.setVideoCount(videoRepository.countByChannelId(channel.getId()));
        return dto;
    }

    //Kanal başkasınınsa ForbiddenException fırlatır (403)
    public ChannelResponseDto updateChannel(Long id, ChannelRequestDto request, Long userId)
    {
        //Değiştirilme/update edilmek istenilen nesneyi bukuyoru varsa atanır yoksa null
        Channel channel=channelRepository.findById(id).orElse(null);

        if(channel==null)
            return null;

        Long ownerId=channel.getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        checkFileUrls(request);

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
    //Kanal başkasınınsa ForbiddenException fırlatır (403)
    public boolean deleteChannelById(Long id, Long userId)
    {
        Channel channel=channelRepository.findById(id).orElse(null);
        if(channel==null)
            return false;

        Long ownerId=channel.getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        channelRepository.deleteById(id);
        return true;
    }

    //Profil ve kapak resmi adresi yalnız bizim MinIO kovamızdan olabilir (dış siteye bağlantı, takip resmi engellenir)
    private void checkFileUrls(ChannelRequestDto request)
    {
        if(!fileService.isOwnFileUrl(request.getProfilePictureUrl()) || !fileService.isOwnFileUrl(request.getBannerImageUrl()))
            throw new BadRequestException();
    }
}
