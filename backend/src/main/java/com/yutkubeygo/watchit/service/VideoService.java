package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.repository.VideoSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoLike;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.mapper.VideoMapper;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.SubscriptionRepository;
import com.yutkubeygo.watchit.repository.VideoLikeRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import com.yutkubeygo.watchit.repository.WatchLaterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VideoService {

    private final VideoMapper videoMapper;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final FileService fileService;
    private final ViewLimiterService viewLimiterService;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final VideoProcessingService videoProcessingService;
    private final WatchLaterRepository watchLaterRepository;
    private final StorageCleanupService storageCleanupService;

    public VideoService(VideoMapper videoMapper, ChannelRepository channelRepository, CategoryRepository categoryRepository, VideoRepository videoRepository, VideoLikeRepository videoLikeRepository, FileService fileService, ViewLimiterService viewLimiterService, SubscriptionRepository subscriptionRepository, NotificationService notificationService, VideoProcessingService videoProcessingService, WatchLaterRepository watchLaterRepository, StorageCleanupService storageCleanupService) {
        this.storageCleanupService = storageCleanupService;
        this.videoMapper = videoMapper;
        this.channelRepository = channelRepository;
        this.categoryRepository = categoryRepository;
        this.videoRepository = videoRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.fileService = fileService;
        this.viewLimiterService = viewLimiterService;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationService = notificationService;
        this.videoProcessingService = videoProcessingService;
        this.watchLaterRepository = watchLaterRepository;
    }

    public VideoResponseDto getVideo(Long id,Long userId)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return null;


        VideoResponseDto dto = videoMapper.toDto(video);
        Long likeCount = videoLikeRepository.countByVideoId(id);
        dto.setLikeCount(likeCount);
        if(userId!=null)
        {
            Optional<VideoLike> myLike =videoLikeRepository.findByUserIdAndVideoId(userId,id);
            if(myLike.isPresent())
                dto.setLikedByMe(true);
            dto.setSavedForLater(watchLaterRepository.existsByUserIdAndVideoId(userId, id));

        }
        return dto;
    }

    //Liste sıralamaları: yeni (en yeni en üstte) ve popüler (en çok izlenen). İkinci sıra id: eşit değerlerde sayfalar arasında kayma olmasın
    public static final Sort NEWEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    public static final Sort POPULAR = Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"));

    public static Sort sortOf(String sort)
    {
        if(sort == null || sort.equals("new"))
            return NEWEST;
        if(sort.equals("popular"))
            return POPULAR;
        if(sort.equals("foryou"))
            return Sort.unsorted();    //sıralamayı sorgu kendisi yapar (listForYou)
        throw new BadRequestException();
    }

    //GET /videos: süzgeçlerin hepsi isteğe bağlı (verilmeyen atlanır). Kanal ya da kategori verilmiş ama yoksa null (controller 404)
    public PageResponseDto<VideoResponseDto> listVideos(Long channelId, Long categoryId, Integer maxDuration, Pageable pageable)
    {
        if(channelId != null && !channelRepository.existsById(channelId))
            return null;
        if(categoryId != null && !categoryRepository.existsById(categoryId))
            return null;

        Specification<Video> spec = VideoSpecs.allOf(
                VideoSpecs.inChannel(channelId), VideoSpecs.inCategory(categoryId), VideoSpecs.maxDuration(maxDuration));
        return toPage(videoRepository.findAll(spec, pageable));
    }

    //"Senin için" (ana sayfa): kişiye göre puanlı sıralama (VideoRepository.findForYou). Kategori yoksa null (404)
    public PageResponseDto<VideoResponseDto> listForYou(Long userId, Long categoryId, Pageable pageable)
    {
        if(categoryId != null && !categoryRepository.existsById(categoryId))
            return null;
        return toPage(videoRepository.findForYou(userId, categoryId, pageable));
    }

    //Abonelik akışı: abone olduğum kanalların videoları, en yeni en üstte (eskiden tarayıcı her kanalı ayrı ayrı çekiyordu)
    public PageResponseDto<VideoResponseDto> getSubscriptionFeed(Long userId, Pageable pageable)
    {
        return toPage(videoRepository.findAll(VideoSpecs.subscribedBy(userId), pageable));
    }

    //Giriş yapan kullanıcının beğendiği videolar, en son beğenilen en üstte
    public PageResponseDto<VideoResponseDto> getLikedVideos(Long userId, Pageable pageable)
    {
        Page<VideoLike> likes = videoLikeRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponseDto.of(likes, videoMapper.toDtoList(likes.map(VideoLike::getVideo).getContent()));
    }

    private PageResponseDto<VideoResponseDto> toPage(Page<Video> page)
    {
        return PageResponseDto.of(page, videoMapper.toDtoList(page.getContent()));
    }

    //Video ve kanalın bütün abonelerine giden bildirimler tek transaction'da yazılır
    @Transactional
    public VideoResponseDto createVideo(VideoRequestDto videoRequestDto,Long userId)
    {
        checkFileUrls(videoRequestDto);

        Video video=videoMapper.toEntity(videoRequestDto);

        Channel channel= channelRepository.findByOwnerId(userId).orElse(null);


        if(channel == null)
        {
            return null;
        }
        

        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);

        if(category==null)
        {
            return null;
        }

        video.setChannel(channel);
        video.setCategory(category);
        //Dosyanın gerçek boyutu MinIO'dan okunur (tarayıcının söylediğine güvenilmez): toplam yükleme kotası bunu toplar
        video.setSizeBytes(fileService.objectSize(video.getVideoUrl()));

        Video savedVideo= videoRepository.save(video);

        List<User> subscribers = subscriptionRepository.findByChannelId(channel.getId()).stream()
                .map(Subscription::getSubscriber)
                .toList();
        notificationService.notifyAll(channel.getOwner(), subscribers, NotificationType.VIDEO_UPLOAD,
                "Yeni video", channel.getChannelName() + " yeni bir video yükledi: " + savedVideo.getTitle());

        //Video arka planda dönüştürülür (kayıt commit edildikten sonra başlar), transkript onun ardından çıkarılır
        videoProcessingService.start(savedVideo);

        return videoMapper.toDto(savedVideo);

    }

    //Video başkasının kanalındaysa ForbiddenException fırlatır (403)
    public VideoResponseDto updateVideo(Long id,VideoRequestDto videoRequestDto,Long userId)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return null;

        Long ownerId=video.getChannel().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        checkFileUrls(videoRequestDto);

        Category category=categoryRepository.findById(videoRequestDto.getCategoryId()).orElse(null);
        if(category==null)
            return null;

        //Kanal değişmez: her kullanıcının tek kanalı var, video zaten onda
        //json`dan gelen degistirilecek veriler burda set edilir
        video.setCategory(category);
        video.setTitle(videoRequestDto.getTitle());
        video.setDescription((videoRequestDto.getDescription()));
        //Yenisiyle değişen kapak ve video dosyasının eskisi depodan silinir (kayıt kaydedildikten sonra)
        StorageCleanupService.Files replaced = new StorageCleanupService.Files();
        StorageCleanupService.addReplaced(replaced, video.getThumbnailUrl(), videoRequestDto.getThumbnailUrl());
        StorageCleanupService.addReplaced(replaced, video.getVideoUrl(), videoRequestDto.getVideoUrl());
        video.setThumbnailUrl(videoRequestDto.getThumbnailUrl());
        boolean fileChanged = !java.util.Objects.equals(video.getVideoUrl(), videoRequestDto.getVideoUrl());
        video.setVideoUrl(videoRequestDto.getVideoUrl());
        if(fileChanged)
            video.setSizeBytes(fileService.objectSize(videoRequestDto.getVideoUrl()));
        video.setDurationInSeconds(videoRequestDto.getDurationInSeconds());

        Video savedVideo=videoRepository.save(video);
        storageCleanupService.deleteAfterCommit(replaced);

        //Video dosyası değiştiyse yeni dosya dönüştürülür, transkript de onun ardından yeniden çıkarılır
        if(fileChanged)
            videoProcessingService.start(savedVideo);

        return videoMapper.toDto(savedVideo);

    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    //Video başkasının kanalındaysa ForbiddenException fırlatır (403)
    public boolean deleteVideo(Long id, Long userId)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return false;

        //Eğer kullanıcı varsa ama sileceği video ile ilgili yetkisi yoksa exception fırlatır
        Long ownerId=video.getChannel().getOwner().getId();
        if(!ownerId.equals(userId))
            throw new ForbiddenException();

        removeVideo(video);
        return true;
    }

    //Yönetici (şikayet üzerine) sahiplik kontrolü olmadan siler; video yoksa false
    public boolean deleteVideoAsAdmin(Long id)
    {
        Video video=videoRepository.findById(id).orElse(null);
        if(video==null)
            return false;
        removeVideo(video);
        return true;
    }

    private void removeVideo(Video video)
    {
        //Dosya adresleri silmeden önce alınır: kayıt gidince adresler de gider
        StorageCleanupService.Files files = new StorageCleanupService.Files().addVideo(video);
        videoRepository.deleteVideoById(video.getId());
        storageCleanupService.deleteAfterCommit(files);
    }




    //Video ve kapak adresi yalnız bizim MinIO kovamızdan olabilir (dış siteye bağlantı, takip resmi engellenir)
    private void checkFileUrls(VideoRequestDto videoRequestDto)
    {
        if(!fileService.isOwnFileUrl(videoRequestDto.getVideoUrl()) || !fileService.isOwnFileUrl(videoRequestDto.getThumbnailUrl()))
            throw new BadRequestException();
    }

    public boolean recordView(Long videoId, Long userId, String ip)
    {
        Video video =videoRepository.findById(videoId).orElse(null);
        if(video==null)
            return false;

        String viewer = (userId==null) ? "ip:"+ip:"userId:"+userId;

        if(viewLimiterService.tryCount(viewer,videoId,video.getDurationInSeconds()))
        {
            videoRepository.incrementViewCount(videoId);
            videoRepository.incrementDailyViews(videoId);    //Stüdyo grafiği (gün gün)
        }

        return true;

    }

    //Başlık, açıklama ve kanal adında arama; metin boşsa veritabanına gitmeden boş sayfa
    public PageResponseDto<VideoResponseDto> searchVideos(String searchText, Pageable pageable)
    {
        String textK=searchText.trim();
        if(textK.isBlank())
            return new PageResponseDto<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), false, 0);
        return toPage(videoRepository.search(escapeLike(textK), pageable));
    }

    //LIKE'ta % (her şey) ve _ (tek karakter) joker; kullanıcı yazdıysa düz karakter olarak aransın diye önüne \ konur.
    //Önce \'in kendisi kaçırılır, yoksa sonradan eklenen \'ler de ikiye katlanırdı.
    static String escapeLike(String text)
    {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

}
