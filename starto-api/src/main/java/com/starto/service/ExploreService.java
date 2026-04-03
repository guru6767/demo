package com.starto.service;

import com.starto.dto.ExploreRequest;
import com.starto.dto.ExploreResponse;
import com.starto.repository.ExploreReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExploreService {

    private final ExploreReportRepository exploreReportRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String openAiKey;

    @Value("${google.ai.api-key}")
    private String geminiKey;

    @Value("${google.maps.api-key}")
    private String googleMapsKey;

    public ExploreResponse analyzeMarket(ExploreRequest request) {
        System.out.println("OPENAI KEY LENGTH: " + (openAiKey != null ? openAiKey.length() : "NULL"));
        System.out.println("OPENAI KEY: " + openAiKey);
        System.out.println("GEMINI KEY: " + geminiKey.substring(0, 10));

        try {
            String locationData = getLocationInsights(request.getLocation());
            System.out.println("LOCATION DATA: " + locationData);

            String gptResponse = callGpt4o(request, locationData);
            System.out.println("GPT RESPONSE: " + gptResponse);

            String geminiResponse = callGemini(request, gptResponse, locationData);
            System.out.println("GEMINI RESPONSE: " + geminiResponse);

            return mergeAndParse(gptResponse, geminiResponse);

        } catch (Exception e) {
            System.out.println("EXPLORE ERROR: " + e.getMessage());
            e.printStackTrace();
            return ExploreResponse.builder()
                    .confidenceScore(0.5)
                    .build();
        }
    }

    private String getLocationInsights(String location) {
        if (googleMapsKey == null || googleMapsKey.isEmpty()) {
            return "Location data unavailable";
        }

        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + location + "&key="
                + googleMapsKey;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("results") && root.get("results").size() > 0) {
                JsonNode firstResult = root.get("results").get(0);
                return "Location: " + location + ", Rating: " + firstResult.get("rating").asText() + ", Types: "
                        + firstResult.get("types").toString();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch location insights", e);
        }
        return "Location data unavailable";
    }

    private String callGpt4o(ExploreRequest request, String locationData) {
        String prompt = buildGptPrompt(request, locationData);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openAiKey);
        headers.set("Content-Type", "application/json");

        // correct format for /v1/chat/completions
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions", // correct endpoint
                HttpMethod.POST,
                entity,
                String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse GPT response", e);
            return "GPT analysis failed";
        }
    }

    private String callGemini(ExploreRequest request, String gptResponse, String locationData) {
        String prompt = buildGeminiPrompt(request, gptResponse, locationData);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                + geminiKey;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return "Gemini validation failed";
        }
    }

    private String buildGptPrompt(ExploreRequest request, String locationData) {
        return "You are a strict JSON generator. ONLY return valid JSON. No explanation, no text.\n\n" +
                "Analyze the market for a " + request.getIndustry() + " business in " + request.getLocation() +
                " with budget " + request.getBudget() + " at stage " + request.getStage() +
                ". Target customer: " + request.getTargetCustomer() +
                ". Location insights: " + locationData + ".\n\n" +
                "Return JSON in this format ONLY:\n" +
                "{\n" +
                "  \"marketDemandScore\": 1-10,\n" +
                "  \"competitors\": [\n" +
                "    {\"name\": \"\", \"location\": \"\", \"stage\": \"\", \"description\": \"\", \"threatLevel\": \"LOW|MEDIUM|HIGH\"}\n"
                +
                "  ],\n" +
                "  \"risks\": [\n" +
                "    {\"title\": \"\", \"description\": \"\", \"severity\": \"LOW|MEDIUM|HIGH\", \"mitigation\": \"\"}\n"
                +
                "  ]\n" +
                "}";
    }

    private String buildGeminiPrompt(ExploreRequest request, String gptResponse, String locationData) {
        return "You are a strict JSON generator. ONLY return valid JSON.\n\n" +
                "Based on this analysis: " + gptResponse + "\n\n" +
                "Return JSON ONLY in this format:\n" +
                "{\n" +
                "  \"governmentSchemes\": [\n" +
                "    {\"name\": \"\", \"body\": \"\", \"benefits\": \"\", \"eligibility\": \"\", \"applyUrl\": \"\"}\n"
                +
                "  ],\n" +
                "  \"actionPlan\": [\n" +
                "    {\"range\": \"\", \"tasks\": [\"\"]}\n" +
                "  ]\n" +
                "}";
    }

    private String cleanJson(String response) {
        return response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }

    private ExploreResponse mergeAndParse(String gptResponse, String geminiResponse) {
        try {
            // JsonNode gptJson = objectMapper.readTree(gptResponse);
            // JsonNode geminiJson = objectMapper.readTree(geminiResponse);

            String cleanGpt = cleanJson(gptResponse);
            String cleanGemini = cleanJson(geminiResponse);

            log.info("GPT CLEAN: {}", cleanGpt);
            log.info("GEMINI CLEAN: {}", cleanGemini);

            JsonNode gptJson = objectMapper.readTree(cleanGpt);
            JsonNode geminiJson = objectMapper.readTree(cleanGemini);

            // Parse Market Demand
            ExploreResponse.MarketDemand marketDemand = new ExploreResponse.MarketDemand();
            marketDemand.setScore(gptJson.get("marketDemandScore").asInt(5));
            marketDemand.setDrivers(List.of("Digital adoption", "Local demand")); // Placeholder
            marketDemand.setSources(List.of("Market reports", "Local surveys"));

            // Parse Competitors
            List<ExploreResponse.Competitor> competitors = objectMapper.convertValue(gptJson.get("competitors"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            ExploreResponse.Competitor.class));

            // Parse Risks
            List<ExploreResponse.Risk> risks = objectMapper.convertValue(gptJson.get("risks"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ExploreResponse.Risk.class));

            // Budget Feasibility (placeholder)
            ExploreResponse.BudgetFeasibility budgetFeasibility = new ExploreResponse.BudgetFeasibility();
            budgetFeasibility.setCanBuild(List.of("MVP", "Prototype"));
            budgetFeasibility.setActualNeed(List.of("Development", "Marketing"));
            budgetFeasibility.setVerdict("Feasible");

            // Parse Government Schemes
            List<ExploreResponse.GovernmentScheme> governmentSchemes = objectMapper
                    .convertValue(geminiJson.get("governmentSchemes"), objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, ExploreResponse.GovernmentScheme.class));

            // Parse Action Plan
            List<ExploreResponse.ActionPhase> actionPlan = objectMapper.convertValue(geminiJson.get("actionPlan"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            ExploreResponse.ActionPhase.class));

            return ExploreResponse.builder()
                    .marketDemand(marketDemand)
                    .competitors(competitors)
                    .risks(risks)
                    .budgetFeasibility(budgetFeasibility)
                    .governmentSchemes(governmentSchemes)
                    .actionPlan(actionPlan)
                    .confidenceScore(0.9)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse responses", e);
            return ExploreResponse.builder()
                    .confidenceScore(0.7)
                    .build();
        }
    }
}