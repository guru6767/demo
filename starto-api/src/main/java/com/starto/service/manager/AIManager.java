package com.starto.service.manager;

import com.starto.service.explore.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AIManager {

    private final AIService aiService;

    public CompletableFuture<String> analyzeWithFallback(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return aiService.analyze(prompt);
            } catch (Exception openAiEx) {
                System.out.println("OpenAI failed: " + openAiEx.getMessage() + "  trying Gemini");
                try {
                    return aiService.validate(prompt);
                } catch (Exception geminiEx) {
                    System.out.println("Both AI services failed, returning default");
                    return "{\"marketDemandScore\": 5, \"competitors\": [], \"risks\": []}";
                }
            }
        });
    }
}