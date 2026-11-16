package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Whisper konteynerine (whisper-servisi/, compose'ta watchit_whisper) HTTP ile istek atar
@Component
public class WhisperTranscriptionClient implements TranscriptionClient {

    //Python servisinin hata cevabı: {"detail": "..."}
    private static final Pattern DETAIL = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");

    private final RestClient restClient;

    public WhisperTranscriptionClient(@Value("${whisper.url}") String whisperUrl,
                                      @Value("${whisper.timeout-minutes}") long timeoutMinutes)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        //10 dakikalık bir video işlemcide birkaç dakika sürer; cevap gelene kadar beklenir
        factory.setReadTimeout(Duration.ofMinutes(timeoutMinutes));
        this.restClient = RestClient.builder().baseUrl(whisperUrl).requestFactory(factory).build();
    }

    @Override
    public TranscriptionResult transcribe(String videoUrl)
    {
        try
        {
            return restClient.post()
                    .uri("/transcribe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", videoUrl))
                    .retrieve()
                    //Servis hata verirse ("Videoda ses bulunamadı" gibi) yalnız açıklamasını al, ham HTTP cevabını değil
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        Matcher m = DETAIL.matcher(body);
                        throw new IllegalStateException(m.find() ? m.group(1) : "Whisper servisi hata verdi (" + response.getStatusCode().value() + ").");
                    })
                    .body(TranscriptionResult.class);
        }
        catch(ResourceAccessException e)
        {
            //Bağlantı kurulamadı ya da süre doldu
            throw new IllegalStateException("Whisper servisine ulaşılamadı.", e);
        }
    }
}
