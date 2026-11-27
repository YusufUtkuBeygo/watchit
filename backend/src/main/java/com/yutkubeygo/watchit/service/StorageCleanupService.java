package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

//Silinen (ya da yenisiyle değiştirilen) kayıtların MinIO'daki dosyalarını siler: video, kapak, HLS paketi, kanal resimleri.
//Neden ayrı bir servis: veritabanı kaydı silinince dosya kendiliğinden gitmiyor; her silme yerinde aynı kuralları tekrar yazmamak için.
@Service
public class StorageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(StorageCleanupService.class);

    private final FileService fileService;
    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final Executor executor;

    @Value("${storage.orphan-cleanup.enabled:true}")
    private boolean orphanCleanupEnabled = true;
    @Value("${storage.orphan-cleanup.after-hours:24}")
    private long orphanAfterHours = 24;

    public StorageCleanupService(FileService fileService, VideoRepository videoRepository, ChannelRepository channelRepository,
                                 @Qualifier("storageExecutor") Executor executor)
    {
        this.fileService = fileService;
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.executor = executor;
    }

    //Silinecek dosyalar: tek dosyalar (video, kapak, resim), HLS paketleri (klasör) ve bölüm kareleri (video id'sine göre klasör)
    public record Files(Set<String> files, Set<String> hlsPackages, Set<Long> chapterFrameVideos)
    {
        public Files()
        {
            this(new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        //Videonun bütün dosyaları. hlsSourceUrl: HLS'nin üretildiği eski dosya; videoUrl'den farklıysa o da artık kimsenin değil
        public Files addVideo(Video video)
        {
            if(video.getId() != null)
                chapterFrameVideos.add(video.getId());
            add(files, video.getVideoUrl());
            add(files, video.getThumbnailUrl());
            add(files, video.getHlsSourceUrl());
            add(hlsPackages, video.getHlsUrl());
            return this;
        }

        //Projeksiyonla gelen adresler (hesap silme)
        public Files addVideo(VideoRepository.VideoFiles video)
        {
            if(video.getId() != null)
                chapterFrameVideos.add(video.getId());
            add(files, video.getVideoUrl());
            add(files, video.getThumbnailUrl());
            add(files, video.getHlsSourceUrl());
            add(hlsPackages, video.getHlsUrl());
            return this;
        }

        public Files addChannel(ChannelRepository.ChannelFiles channel)
        {
            add(files, channel.getProfilePictureUrl());
            add(files, channel.getBannerImageUrl());
            return this;
        }

        public Files addChannel(Channel channel)
        {
            add(files, channel.getProfilePictureUrl());
            add(files, channel.getBannerImageUrl());
            return this;
        }

        public Files addFile(String url)
        {
            add(files, url);
            return this;
        }

        private static void add(Set<String> set, String url)
        {
            if(url != null && !url.isBlank())
                set.add(url);
        }

        public boolean isEmpty()
        {
            return files.isEmpty() && hlsPackages.isEmpty() && chapterFrameVideos.isEmpty();
        }
    }

    //Dosyaları, veritabanı işlemi başarıyla bittikten (commit) SONRA siler.
    //Neden sonra: önce dosyayı silip sonra veritabanı işlemi geri alınsaydı (rollback), kayıt dururdu ama dosyası olmazdı (kırık video).
    //Ters durum (kayıt silindi, dosya silinemedi) yalnız depoda yer kaplar; daha zararsız olan bu.
    public void deleteAfterCommit(Files files)
    {
        if(files == null || files.isEmpty())
            return;
        //Arka planda: 300 parçalık bir HLS paketini silmek birkaç saniye sürebilir, kullanıcının isteği beklemesin
        Runnable job = () -> executor.execute(() -> deleteNow(files));
        if(TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit()
                {
                    job.run();
                }
            });
        }
        else
        {
            //İşlem (transaction) yoksa silme zaten veritabanına yazıldı
            job.run();
        }
    }

    //Her dosyayı ayrı dener: biri silinemezse diğerleri yine silinsin
    void deleteNow(Files files)
    {
        for(String url : files.files())
        {
            //Aynı dosyayı başka bir kayıt hâlâ kullanıyorsa (ör. demo verisinde aynı test videosu, aynı kapak) dokunma
            if(isStillUsed(url))
                continue;
            try
            {
                fileService.deleteOwnFile(url);
            }
            catch(Exception e)
            {
                log.warn("Dosya silinemedi (depoda sahipsiz kaldı): {}: {}", url, e.getMessage());
            }
        }
        for(String hlsUrl : files.hlsPackages())
        {
            if(videoRepository.existsByHlsUrl(hlsUrl))
                continue;
            try
            {
                fileService.deleteHlsPackage(hlsUrl);
            }
            catch(Exception e)
            {
                log.warn("HLS paketi silinemedi (depoda sahipsiz kaldı): {}: {}", hlsUrl, e.getMessage());
            }
        }
        //Bölüm kareleri yalnız o videonun: başka kayıt kullanmaz, kontrol gerekmez
        for(Long videoId : files.chapterFrameVideos())
        {
            try
            {
                fileService.deleteChapterFrames(videoId);
            }
            catch(Exception e)
            {
                log.warn("Bölüm kareleri silinemedi (depoda sahipsiz kaldı): video {}: {}", videoId, e.getMessage());
            }
        }
    }

    //Sahipsiz yüklemeler: tarayıcı dosyayı MinIO'ya yükleyip video/kanal kaydını hiç oluşturmazsa (pencere kapandı,
    //hata oldu ya da biri bilerek depoyu şişirmek istiyor) dosya sonsuza kadar kalırdı. Her 6 saatte bir,
    //yükleme klasöründe (yuklemeler/) orphanAfterHours'tan eski ve hiçbir kayıtta geçmeyen dosyalar silinir.
    //Bekleme süresi: o an formu doldurmakta olan kullanıcının dosyası silinmesin
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 15 * 60 * 1000L)
    public void cleanOrphanUploads()
    {
        if(!orphanCleanupEnabled)
            return;
        int deleted = 0;
        try
        {
            for(String url : fileService.listUploadsOlderThan(ZonedDateTime.now().minusHours(orphanAfterHours)))
            {
                if(isStillUsed(url))
                    continue;
                try
                {
                    fileService.deleteOwnFile(url);
                    deleted++;
                }
                catch(Exception e)
                {
                    log.warn("Sahipsiz dosya silinemedi: {}: {}", url, e.getMessage());
                }
            }
        }
        catch(Exception e)
        {
            log.warn("Sahipsiz dosya temizliği yapılamadı: {}", e.getMessage());
            return;
        }
        if(deleted > 0)
            log.info("Sahipsiz yükleme temizliği: {} dosya silindi", deleted);
    }

    //Dosya adresi herhangi bir videoda ya da kanalda geçiyor mu
    private boolean isStillUsed(String url)
    {
        return videoRepository.existsByVideoUrl(url)
                || videoRepository.existsByThumbnailUrl(url)
                || videoRepository.existsByHlsSourceUrl(url)
                || channelRepository.existsByProfilePictureUrl(url)
                || channelRepository.existsByBannerImageUrl(url);
    }

    //Güncellemede eski dosya yenisiyle değiştiyse eskisini sil (aynıysa ya da boşsa bir şey yapmaz)
    public static void addReplaced(Files files, String oldUrl, String newUrl)
    {
        if(oldUrl != null && !Objects.equals(oldUrl, newUrl))
            files.addFile(oldUrl);
    }

    public static Files ofVideos(Collection<Video> videos)
    {
        Files files = new Files();
        videos.forEach(files::addVideo);
        return files;
    }
}
