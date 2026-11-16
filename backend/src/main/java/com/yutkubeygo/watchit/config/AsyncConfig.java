package com.yutkubeygo.watchit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

//Arka plan işleri (@Async). Transkript işi uzun sürer (dakikalar); istek beklemesin diye ayrı bir iş parçacığında çalışır.
@Configuration
@EnableAsync
public class AsyncConfig {

    //Transkriptler sırayla, birer birer işlenir (Whisper işlemciyi zaten tam kullanıyor); sıradakiler kuyrukta bekler
    @Bean(name = "transcriptExecutor")
    public Executor transcriptExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("transkript-");
        executor.initialize();
        return executor;
    }
}
