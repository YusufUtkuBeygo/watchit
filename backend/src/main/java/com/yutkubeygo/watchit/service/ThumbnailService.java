package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

//Otomatik kapak: kapaksız yüklenen videoya, dönüştürme bitince medya servisi videodan bir kare seçer (POST /thumbnail).
//Kapak isteğe bağlı bir süs: çıkarılamazsa video yine oynar, kart kapak yerine düz zemin gösterir.
@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

    private final VideoRepository videoRepository;
    private final FileService fileService;
    private final RestClient restClient;

    public ThumbnailService(VideoRepository videoRepository, FileService fileService, @Value("${whisper.url}") String mediaServiceUrl)
    {
        this.videoRepository = videoRepository;
        this.fileService = fileService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        //Kapak için video indirilir; uzun videoda indirme bir iki dakika sürebilir
        factory.setReadTimeout(Duration.ofMinutes(10));
        this.restClient = RestClient.builder().baseUrl(mediaServiceUrl).requestFactory(factory).build();
    }

    //Video kapaksızsa kapak üretip yazar. Bu arada sahibi kapak eklediyse üretilen dosya silinir (onunki kalır)
    public void ensureThumbnail(Long videoId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null || hasText(video.getThumbnailUrl()) || !hasText(video.getVideoUrl()))
            return;

        Path file = null;
        try
        {
            file = fetchThumbnail(video.getVideoUrl());
            String url = fileService.uploadThumbnail(file);
            if(videoRepository.setThumbnailIfMissing(videoId, url) == 0)
                fileService.deleteOwnFile(url);
            else
                log.info("Otomatik kapak yazıldı: video {}", videoId);
        }
        catch(Exception e)
        {
            log.warn("Otomatik kapak çıkarılamadı: video {}: {}", videoId, e.getMessage());
        }
        finally
        {
            if(file != null)
            {
                try { Files.deleteIfExists(file); } catch(IOException ignored) { }
            }
        }
    }

    private Path fetchThumbnail(String videoUrl) throws IOException
    {
        Path file = Files.createTempFile("watchit-kapak-", ".jpg");
        return restClient.post()
                .uri("/thumbnail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("url", videoUrl))
                .exchange((request, response) -> {
                    if(response.getStatusCode().isError())
                    {
                        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        Files.deleteIfExists(file);
                        throw new IllegalStateException(MediaServiceError.message(body, response.getStatusCode().value()));
                    }
                    try(InputStream in = response.getBody())
                    {
                        Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return file;
                });
    }

    private static boolean hasText(String s)
    {
        return s != null && !s.isBlank();
    }
}
