package com.starto.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExploreResponse {
    private MarketDemand marketDemand;
    private List<Competitor> competitors;
    private List<Risk> risks;
    private BudgetFeasibility budgetFeasibility;
    private List<GovernmentScheme> governmentSchemes;
    private List<ActionPhase> actionPlan;
    private Double confidenceScore;

    @Data
    public static class MarketDemand {
        private int score;
        private List<String> drivers;
        private List<String> sources;
    }

    @Data
    public static class Competitor {
        private String name;
        private String location;
        private String stage;
        private String description;
        private String threatLevel;
    }

    @Data
    public static class Risk {
        private String title;
        private String description;
        private String severity;
        private String mitigation;
    }

    @Data
    public static class BudgetFeasibility {
        private List<String> canBuild;
        private List<String> actualNeed;
        private String verdict;
    }

    @Data
    public static class GovernmentScheme {
        private String name;
        private String body;
        private String benefits;
        private String eligibility;
        private String applyUrl;
    }

    @Data
    public static class ActionPhase {
        private String range;
        private List<String> tasks;
    }
}
