package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscodeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

//Medya servisine (whisper-servisi/, compose'ta watchit_whisper) POST /transcode atar.
//Cevap dönüşmüş videonun kendisidir: belleğe alınmadan, parça parça geçici dosyaya yazılır.
@Component
public class MediaServiceVideoProcessingClient implements VideoProcessingClient {

    private final RestClient restClient;

    public MediaServiceVideoProcessingClient(@Value("${whisper.url}") String mediaServiceUrl,
                                             @Value("${video-processing.timeout-minutes}") long timeoutMinutes)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        //Uzun bir videoyu işlemcide yeniden kodlamak dakikalar sürer; cevap gelene kadar beklenir
        factory.setReadTimeout(Duration.ofMinutes(timeoutMinutes));
        this.restClient = RestClient.builder().baseUrl(mediaServiceUrl).requestFactory(factory).build();
    }

    @Override
    public TranscodeResult transcode(String videoUrl)
    {
        Path file;
        try
        {
            file = Files.createTempFile("watchit-video-", ".mp4");
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }

        try
        {
            return restClient.post()
                    .uri("/transcode")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", videoUrl))
                    .exchange((request, response) -> {
                        if(response.getStatusCode().isError())
                        {
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new IllegalStateException(MediaServiceError.message(body, response.getStatusCode().value()));
                        }
                        try(InputStream in = response.getBody())
                        {
                            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                        }
                        HttpHeaders h = response.getHeaders();
                        return new TranscodeResult(file, h.getFirst("X-Mode"), parseDouble(h.getFirst("X-Duration")),
                                parseInt(h.getFirst("X-Width")), parseInt(h.getFirst("X-Height")));
                    });
        }
        catch(ResourceAccessException e)
        {
            deleteQuietly(file);
            //Bağlantı kurulamadı ya da süre doldu
            throw new IllegalStateException("Medya servisine ulaşılamadı.", e);
        }
        catch(RuntimeException e)
        {
            deleteQuietly(file);
            throw e;
        }
    }

    private static double parseDouble(String value)
    {
        try
        {
            return value == null ? 0 : Double.parseDouble(value);
        }
        catch(NumberFormatException e)
        {
            return 0;
        }
    }

    private static Integer parseInt(String value)
    {
        try
        {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

    private static void deleteQuietly(Path file)
    {
        try
        {
            Files.deleteIfExists(file);
        }
        catch(IOException ignored)
        {
        }
    }
}
