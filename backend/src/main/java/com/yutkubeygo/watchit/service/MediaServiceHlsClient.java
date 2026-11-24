package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.HlsResult;
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

//Medya servisine (whisper-servisi/) POST /hls atar. Cevap bütün kalitelerin parçalarını içeren bir zip'tir:
//belleğe alınmadan, parça parça geçici dosyaya yazılır (720p'lik 10 dk video yüzlerce MB tutar).
@Component
public class MediaServiceHlsClient implements HlsClient {

    private final RestClient restClient;

    public MediaServiceHlsClient(@Value("${whisper.url}") String mediaServiceUrl,
                                 @Value("${video-processing.timeout-minutes}") long timeoutMinutes)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        //Dört kaliteyi işlemcide kodlamak uzun sürebilir; dönüştürmeyle aynı süre sınırı
        factory.setReadTimeout(Duration.ofMinutes(timeoutMinutes));
        this.restClient = RestClient.builder().baseUrl(mediaServiceUrl).requestFactory(factory).build();
    }

    @Override
    public HlsResult createHls(String videoUrl)
    {
        Path zip;
        try
        {
            zip = Files.createTempFile("watchit-hls-", ".zip");
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }

        try
        {
            return restClient.post()
                    .uri("/hls")
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
                            Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
                        }
                        HttpHeaders h = response.getHeaders();
                        return new HlsResult(zip, h.getFirst("X-Variants"), parseDouble(h.getFirst("X-Duration")));
                    });
        }
        catch(ResourceAccessException e)
        {
            deleteQuietly(zip);
            throw new IllegalStateException("Medya servisine ulaşılamadı.", e);
        }
        catch(RuntimeException e)
        {
            deleteQuietly(zip);
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
