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
    private final StorageCleanupService storageCleanupService;

    public ChannelService(ChannelMapper channelMapper, ChannelRepository channelRepository, UserRepository userRepository, SubscriptionRepository subscriptionRepository, FileService fileService, VideoRepository videoRepository, StorageCleanupService storageCleanupService) {
        this.storageCleanupService = storageCleanupService;
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
        //Yenisiyle değişen resimlerin eskisi depodan silinir
        StorageCleanupService.Files replaced = new StorageCleanupService.Files();
        StorageCleanupService.addReplaced(replaced, channel.getBannerImageUrl(), request.getBannerImageUrl());
        StorageCleanupService.addReplaced(replaced, channel.getProfilePictureUrl(), request.getProfilePictureUrl());
        channel.setBannerImageUrl(request.getBannerImageUrl());
        channel.setProfilePictureUrl(request.getProfilePictureUrl());
        channel.setDescription(request.getDescription());

        //Değişiklikleri kaydediyoruz
        Channel savedChannel=channelRepository.save(channel);
        storageCleanupService.deleteAfterCommit(replaced);
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

        //Kanal silinince videoları da silinir (ON DELETE CASCADE, V4); dosyalarının adresleri önceden toplanır
        StorageCleanupService.Files files = StorageCleanupService.ofVideos(videoRepository.findByChannelIdOrderByCreatedAtDesc(id))
                .addChannel(channel);
        channelRepository.deleteById(id);
        storageCleanupService.deleteAfterCommit(files);
        return true;
    }

    //Profil ve kapak resmi adresi yalnız bizim MinIO kovamızdan olabilir (dış siteye bağlantı, takip resmi engellenir)
    private void checkFileUrls(ChannelRequestDto request)
    {
        if(!fileService.isOwnFileUrl(request.getProfilePictureUrl()) || !fileService.isOwnFileUrl(request.getBannerImageUrl()))
            throw new BadRequestException();
    }

    //Stüdyo: kendi kanalımın toplamları ve son "days" günün günlük görüntülenmesi. Kanalım yoksa null (404)
    public com.yutkubeygo.watchit.dto.ChannelStatsDto getMyStats(Long userId, int days)
    {
        Channel channel = channelRepository.findByOwnerId(userId).orElse(null);
        if(channel == null)
            return null;
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate since = today.minusDays(days - 1L);
        java.util.Map<java.time.LocalDate, Long> byDay = new java.util.HashMap<>();
        for(Object[] row : videoRepository.dailyViewsOfChannel(channel.getId(), since))
        {
            //Sürücü günü LocalDate (yeni) ya da java.sql.Date (eski) olarak verebilir
            java.time.LocalDate day = row[0] instanceof java.sql.Date sql ? sql.toLocalDate() : (java.time.LocalDate) row[0];
            byDay.put(day, ((Number) row[1]).longValue());
        }
        List<com.yutkubeygo.watchit.dto.ChannelStatsDto.Day> daily = new java.util.ArrayList<>();
        long period = 0;
        for(java.time.LocalDate d = since; !d.isAfter(today); d = d.plusDays(1))
        {
            long views = byDay.getOrDefault(d, 0L);
            period += views;
            daily.add(new com.yutkubeygo.watchit.dto.ChannelStatsDto.Day(d, views));
        }
        return new com.yutkubeygo.watchit.dto.ChannelStatsDto(videoRepository.totalViewsOfChannel(channel.getId()),
                subscriptionRepository.countByChannelId(channel.getId()), videoRepository.countByChannelId(channel.getId()),
                period, daily);
    }
}
