package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.entity.VideoChapter;
import com.yutkubeygo.watchit.repository.VideoChapterRepository;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//Bölüm kareleri: her bölümün başından (2 sn sonrasından) bir küçük resim. Medya servisi (POST /frames) kareleri
//tek zip'te verir, burada MinIO'ya yüklenip bölüme yazılır.
//Bölümler gibi isteğe bağlı bir süs: kare çıkarılamazsa bölüm resimsiz görünür, açılışta yeniden denenir.
@Service
public class ChapterFrameService {

    private static final Logger log = LoggerFactory.getLogger(ChapterFrameService.class);

    //Zip'teki adlar: 000.jpg, 001.jpg... (istek sırası). Başka bir ad gelirse kabul edilmez
    private static final Pattern FRAME_ENTRY = Pattern.compile("(\\d{3})\\.jpg");

    private final VideoChapterRepository chapterRepository;
    private final VideoRepository videoRepository;
    private final FileService fileService;
    private final RestClient restClient;

    public ChapterFrameService(VideoChapterRepository chapterRepository, VideoRepository videoRepository, FileService fileService,
                               @Value("${whisper.url}") String mediaServiceUrl)
    {
        this.chapterRepository = chapterRepository;
        this.videoRepository = videoRepository;
        this.fileService = fileService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        //Video bir kez indirilir; uzun videoda bir iki dakika sürebilir
        factory.setReadTimeout(Duration.ofMinutes(10));
        this.restClient = RestClient.builder().baseUrl(mediaServiceUrl).requestFactory(factory).build();
    }

    //Karesi olmayan bölümlerin karelerini çıkarır. Hata yalnız loglanır (çağıran bölüm işi bozulmasın)
    public void generate(Long videoId)
    {
        Video video = videoRepository.findById(videoId).orElse(null);
        if(video == null || video.getVideoUrl() == null || video.getVideoUrl().isBlank())
            return;
        List<VideoChapter> missing = chapterRepository.findByVideoIdOrderByStartSecondsAsc(videoId).stream()
                .filter(c -> c.getThumbnailUrl() == null)
                .toList();
        if(missing.isEmpty())
            return;

        Path zip = null;
        List<String> uploaded = new ArrayList<>();
        try
        {
            zip = fetchFrames(video.getVideoUrl(), missing.stream().map(VideoChapter::getStartSeconds).toList());
            try(ZipFile zipFile = new ZipFile(zip.toFile()))
            {
                for(ZipEntry entry : java.util.Collections.list(zipFile.entries()))
                {
                    Matcher m = FRAME_ENTRY.matcher(entry.getName());
                    int index = m.matches() ? Integer.parseInt(m.group(1)) : -1;
                    if(index < 0 || index >= missing.size())
                        throw new IllegalStateException("Kare paketinde beklenmeyen dosya: " + entry.getName());
                    String url;
                    try(InputStream in = zipFile.getInputStream(entry))
                    {
                        url = fileService.uploadChapterFrame(videoId, in, entry.getSize());
                    }
                    uploaded.add(url);
                    //Bölüm bu arada yenilendiyse (ya da video silindiyse) kare kimsenin değil: hemen sil
                    if(chapterRepository.setThumbnailIfMissing(missing.get(index).getId(), url) == 0)
                        fileService.deleteOwnFile(url);
                }
            }
            log.info("Bölüm kareleri yazıldı: video {}, {} kare", videoId, uploaded.size());
        }
        catch(Exception e)
        {
            log.warn("Bölüm kareleri çıkarılamadı: video {}: {}", videoId, e.getMessage());
        }
        finally
        {
            if(zip != null)
            {
                try { Files.deleteIfExists(zip); } catch(IOException ignored) { }
            }
        }
    }

    private Path fetchFrames(String videoUrl, List<Integer> times) throws IOException
    {
        Path file = Files.createTempFile("watchit-kareler-", ".zip");
        return restClient.post()
                .uri("/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("url", videoUrl, "times", times))
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
}
