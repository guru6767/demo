package com.starto.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsClient {

    private final WebClient webClient = WebClient.create();

    @Value("${google.maps.api-key}")
    private String apiKey;

    public String fetchInsights(String location) {

        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + location + "&key=" + apiKey;

        try {
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5)) //  faster timeout
                    .block();

            return response;

        } catch (Exception e) {
            log.warn("Google Maps API failed", e);
            return "Location data unavailable";
        }
    }
}