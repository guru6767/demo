package com.starto.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;


import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

    private final WebClient webClient = WebClient.create();

    @Value("${openai.api-key}")
    private String apiKey;

//     @Value("${openai.api-key}")
// private String apiKey;

@jakarta.annotation.PostConstruct
public void init() {
    System.out.println("OPENAI KEY LENGTH: " + (apiKey != null ? apiKey.length() : "NULL"));
    System.out.println("OPENAI KEY PREFIX: " + (apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) : apiKey));
}

    public String analyze(String prompt) {

        try {
           return webClient.post()
        .uri("https://api.openai.com/v1/chat/completions")
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .bodyValue(Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        ))
        .retrieve()   //  IMPORTANT
        .bodyToMono(String.class)
        .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
        .timeout(Duration.ofSeconds(30))
        .block();

        } catch (Exception e) {
            log.error("OpenAI API failed", e);
            throw new RuntimeException("OpenAI service unavailable");
        }
    }
}