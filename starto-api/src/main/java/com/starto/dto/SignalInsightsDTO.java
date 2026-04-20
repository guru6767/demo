package com.starto.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SignalInsightsDTO {
    private long totalViews;
    private long totalResponses;
    private long totalOffers;
    private long followerViews;
   private long nonFollowerViews;
    private List<Map<String, Object>> viewsOverTime; // [{date, count}, ...]
}