package com.traderecon.forge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService executorService(
            @Value("${processing.thread-pool-size:8}") int threadPoolSize
    ) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}