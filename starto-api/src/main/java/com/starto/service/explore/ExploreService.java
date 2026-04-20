package com.starto.service.explore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starto.dto.ExploreRequest;
import com.starto.dto.ExploreResponse;
import com.starto.model.AiUsage;
import com.starto.repository.AiUsageRepository;
import com.starto.service.explore.LocationService;
import com.starto.service.manager.AIManager;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.starto.service.WebSocketService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExploreService {

    private final AIManager aiManager;
    private final LocationService locationService;
    private final WebSocketService webSocketService;
    private final AiUsageRepository aiUsageRepository;

    @Cacheable(
        value = "exploreCache",
        key = "#request.location + '-' + #request.industry + '-' + #request.stage"
    )
    public ExploreResponse analyzeMarket(ExploreRequest request, String userId) {
        System.out.println("ANALYZE MARKET CALLED");
        try {
            webSocketService.send("/topic/explore/" + userId,
                    Map.of("status", "started", "message", "Fetching location data...", "progress", 10));

            String locationData = locationService.getInsights(request.getLocation());
            System.out.println("LOCATION DATA: " + locationData);

            webSocketService.send("/topic/explore/" + userId,
                    Map.of("status", "processing", "message", "Analyzing market with AI...", "progress", 40));

            String prompt = buildGptPrompt(request, locationData);
            System.out.println("PROMPT BUILT");

            webSocketService.send("/topic/explore/" + userId,
                    Map.of("status", "processing", "message", "Generating insights...", "progress", 70));

            String aiResponse = aiManager.analyzeWithFallback(prompt).get();
            System.out.println("AI RESPONSE: " + aiResponse);

            webSocketService.send("/topic/explore/" + userId,
                    Map.of("status", "complete", "message", "Analysis complete!", "progress", 100));

            return parse(aiResponse, aiResponse);

        } catch (Exception e) {
            System.out.println("ANALYZE EXCEPTION: " + e.getMessage());
            webSocketService.send("/topic/explore/" + userId,
                    Map.of("status", "error", "message", "Analysis failed", "progress", 0));
            e.printStackTrace();
            return ExploreResponse.builder().confidenceScore(0.0).build();
        }
    }

    private String buildGptPrompt(ExploreRequest req, String locationData) {
        return "You are a strict JSON generator. Return ONLY valid JSON, no explanation, no markdown.\n\n" +
                "Analyze market for " + req.getIndustry() +
                " in " + req.getLocation() +
                " with budget " + req.getBudget() +
                " at stage " + req.getStage() +
                ". Target customer: " + req.getTargetCustomer() +
                ". Location context: " + locationData + "\n\n" +
                "Return this exact JSON structure:\n" +
                "{\n" +
                "  \"marketDemandScore\": <1-10>,\n" +
                "  \"competitors\": [{\"name\": \"\", \"location\": \"\", \"stage\": \"\", \"description\": \"\", \"threatLevel\": \"LOW|MEDIUM|HIGH\"}],\n" +
                "  \"risks\": [{\"title\": \"\", \"description\": \"\", \"severity\": \"LOW|MEDIUM|HIGH\", \"mitigation\": \"\"}],\n" +
                "  \"budgetFeasibility\": {\"canBuild\": [\"item1\", \"item2\"], \"actualNeed\": [\"item1\", \"item2\"], \"verdict\": \"Feasible|Tight|Infeasible\"},\n" +
                "  \"governmentSchemes\": [{\"name\": \"\", \"body\": \"\", \"benefits\": \"\", \"eligibility\": \"\", \"applyUrl\": \"\"}],\n" +
                "  \"actionPlan\": [{\"range\": \"0-3 months\", \"tasks\": [\"task1\", \"task2\"]}]\n" +
                "}";
    }

    private ExploreResponse parse(String rawResponse, String gemini) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawResponse);

            String content = null;

            if (root.has("choices")) {
                content = root.get("choices").get(0)
                        .get("message")
                        .get("content")
                        .asText();
            } else if (root.has("candidates")) {
                content = root.get("candidates").get(0)
                        .get("content")
                        .get("parts").get(0)
                        .get("text")
                        .asText();
            } else {
                content = rawResponse;
            }

            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode data = mapper.readTree(content);

            ExploreResponse.MarketDemand marketDemand = new ExploreResponse.MarketDemand();
            marketDemand.setScore(data.has("marketDemandScore") ? data.get("marketDemandScore").asInt(5) : 7);
            marketDemand.setDrivers(List.of("Digital adoption", "Cloud growth", "Local demand"));
            marketDemand.setSources(List.of("AI Analysis", "Market Research"));

            List<ExploreResponse.Competitor> competitors = data.has("competitors")
                    ? mapper.convertValue(data.get("competitors"),
                        mapper.getTypeFactory().constructCollectionType(List.class, ExploreResponse.Competitor.class))
                    : List.of();

            List<ExploreResponse.Risk> risks = data.has("risks")
                    ? mapper.convertValue(data.get("risks"),
                        mapper.getTypeFactory().constructCollectionType(List.class, ExploreResponse.Risk.class))
                    : List.of();

            ExploreResponse.BudgetFeasibility budgetFeasibility = null;
            if (data.has("budgetFeasibility")) {
                JsonNode bf = data.get("budgetFeasibility");
                budgetFeasibility = new ExploreResponse.BudgetFeasibility();
                budgetFeasibility.setVerdict(bf.has("verdict") ? bf.get("verdict").asText() : "Unknown");
                budgetFeasibility.setCanBuild(bf.has("canBuild")
                        ? mapper.convertValue(bf.get("canBuild"),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class))
                        : List.of());
                budgetFeasibility.setActualNeed(bf.has("actualNeed")
                        ? mapper.convertValue(bf.get("actualNeed"),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class))
                        : List.of());
            }

            List<ExploreResponse.GovernmentScheme> governmentSchemes = data.has("governmentSchemes")
                    ? mapper.convertValue(data.get("governmentSchemes"),
                        mapper.getTypeFactory().constructCollectionType(List.class, ExploreResponse.GovernmentScheme.class))
                    : List.of();

            List<ExploreResponse.ActionPhase> actionPlan = data.has("actionPlan")
                    ? mapper.convertValue(data.get("actionPlan"),
                        mapper.getTypeFactory().constructCollectionType(List.class, ExploreResponse.ActionPhase.class))
                    : List.of();

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
            log.error("Failed to parse AI response: {}", e.getMessage());
            return ExploreResponse.builder().confidenceScore(0.5).build();
        }
    }

    public int getTodayUsage(UUID userId) {
    return aiUsageRepository
            .findByUserIdAndDate(userId, LocalDate.now())
            .map(AiUsage::getUsedCount)
            .orElse(0);
}

@Transactional
public void incrementUsage(UUID userId) {

    LocalDate today = LocalDate.now();

    AiUsage usage = aiUsageRepository
            .findByUserIdAndDate(userId, today)
            .orElse(
                AiUsage.builder()
                        .userId(userId)
                        .date(today)
                        .usedCount(0)
                        .build()
            );

    usage.setUsedCount(usage.getUsedCount() + 1);

    aiUsageRepository.save(usage);
}
}