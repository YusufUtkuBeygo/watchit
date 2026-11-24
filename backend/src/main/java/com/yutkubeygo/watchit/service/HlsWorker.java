package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.HlsResult;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

//Arka plan işçisi: videoyu medya servisine HLS'ye çevirtir, paketi MinIO'ya açar, videoya bağlar, eski paketi siler.
//Dönüştürmeyle aynı tek iş parçacığında sırayla çalışır (videoProcessingExecutor): ikisi aynı anda işlemciyi boğmasın.
//Ayrı sınıf, çünkü @Async yalnız başka bir sınıftan çağrılınca çalışır.
@Component
public class HlsWorker {

    private static final Logger log = LoggerFactory.getLogger(HlsWorker.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final VideoRepository videoRepository;
    private final HlsClient client;
    private final FileService fileService;

    public HlsWorker(VideoRepository videoRepository, HlsClient client, FileService fileService)
    {
        this.videoRepository = videoRepository;
        this.client = client;
        this.fileService = fileService;
    }

    @Async("videoProcessingExecutor")
    public void processAsync(Long videoId)
    {
        process(videoId);
    }

    //PENDING -> PROCESSING -> READY (oynatıcı HLS'ye geçer) ya da FAILED (MP4 oynamaya devam eder)
    public void process(Long videoId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null || video.getHlsStatus() != VideoProcessingStatus.PENDING || video.getVideoUrl() == null)
            return;
        //Dönüştürme sürüyorsa dosya birazdan değişecek: dönüştürme bitince HLS yeniden sıraya girer
        if(VideoProcessingService.isRunning(video))
            return;

        String sourceUrl = video.getVideoUrl();
        String oldHlsUrl = video.getHlsUrl();
        if(videoRepository.changeHlsStatus(videoId, VideoProcessingStatus.PENDING, VideoProcessingStatus.PROCESSING) == 0)
            return;
        log.info("HLS başladı: video {}", videoId);

        HlsResult result = null;
        try
        {
            result = client.createHls(sourceUrl);
            String hlsUrl = fileService.uploadHlsPackage(result.zip());

            if(videoRepository.finishHls(videoId, sourceUrl, hlsUrl, VideoProcessingStatus.READY) == 0)
            {
                //Sahibi bu arada dosyayı değiştirdi ya da video silindi: bu paket artık kimsenin değil
                deletePackage(hlsUrl);
                log.info("HLS sonucu atıldı: video {} bu arada değişti", videoId);
                return;
            }
            log.info("HLS bitti: video {}, kaliteler {}", videoId, result.variants());
            //Eski dosyadan üretilmiş paket artık kullanılmıyor
            if(oldHlsUrl != null && !oldHlsUrl.equals(hlsUrl))
                deletePackage(oldHlsUrl);
        }
        catch(Exception e)
        {
            String message = fit("HLS üretilemedi: " + e.getMessage());
            log.warn("HLS başarısız: video {}: {}", videoId, message);
            videoRepository.failHls(videoId, sourceUrl, VideoProcessingStatus.FAILED, message);
        }
        finally
        {
            if(result != null)
                deleteTempFile(result);
        }
    }

    private void deletePackage(String hlsUrl)
    {
        try
        {
            fileService.deleteHlsPackage(hlsUrl);
        }
        catch(Exception e)
        {
            //Video yine çalışıyor; paket depoda sahipsiz kalır
            log.warn("HLS paketi silinemedi: {}: {}", hlsUrl, e.getMessage());
        }
    }

    private static void deleteTempFile(HlsResult result)
    {
        try
        {
            Files.deleteIfExists(result.zip());
        }
        catch(IOException e)
        {
            log.warn("Geçici dosya silinemedi: {}", result.zip());
        }
    }

    private static String fit(String text)
    {
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH - 1) + "…";
    }
}
