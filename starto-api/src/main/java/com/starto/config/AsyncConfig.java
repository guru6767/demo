package com.starto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("aiExecutor")
    public Executor executor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(5);
        ex.setMaxPoolSize(10);
        ex.setQueueCapacity(50);
        ex.initialize();
        // ← THIS wraps the executor to propagate SecurityContext to async threads
        return new DelegatingSecurityContextAsyncTaskExecutor(ex);
    }
}