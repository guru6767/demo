package com.starto.service.explore;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.starto.service.external.OpenAIClient;
import com.starto.service.external.GeminiClient;


@Service
@RequiredArgsConstructor
public class AIService {

    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;

    public String analyze(String prompt) {
        return openAIClient.analyze(prompt);
    }

    public String validate(String prompt) {
        return geminiClient.validate(prompt);
    }
}