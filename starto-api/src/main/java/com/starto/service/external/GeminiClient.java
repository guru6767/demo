package com.starto.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.util.retry.Retry;
import java.time.Duration;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final WebClient webClient = WebClient.create();

    @Value("${google.ai.api-key}")
    private String apiKey;

    public String validate(String prompt) {

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map<String, Object> request = Map.of(
        "contents", List.of(
            Map.of(
                "parts", List.of(
                    Map.of("text", prompt)
                )
            )
        )
    );

        try {
            return webClient.post()
    .uri(url)
    .bodyValue(request)
    .retrieve()

    //  Log actual error from Gemini
    .onStatus(status -> status.isError(), response ->
        response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("Gemini error body: {}", body);
                return Mono.error(new RuntimeException(body));
            })
    )

    .bodyToMono(String.class)

    //  Retry 2 times if failed
    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))

    //  Increase timeout
    .timeout(Duration.ofSeconds(20))

    //  Fallback (MOST IMPORTANT)
    .onErrorResume(e -> {
    log.error("Gemini failed: {}", e.getMessage());
    return Mono.just("{\"marketDemandScore\":5,\"competitors\":[],\"risks\":[]}");
})

    .block();   // keep for now (later we remove)

        } catch (Exception e) {
            log.error("Gemini API failed", e);
            throw new RuntimeException("Gemini service unavailable");
        }
    }
}