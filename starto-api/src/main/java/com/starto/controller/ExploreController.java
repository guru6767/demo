package com.starto.controller;

import com.starto.dto.ExploreRequest;
import com.starto.model.User;
import com.starto.enums.Plan;
import com.starto.service.explore.ExploreService;
import com.starto.service.PlanService;
import com.starto.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;
    private final UserService userService;
    private final PlanService planService;

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return userService
                .getUserByFirebaseUid(authentication.getPrincipal().toString())
                .orElse(null);
    }

    @PostMapping("/analyze")
    public CompletableFuture<ResponseEntity<Object>> analyze(
            Authentication authentication,
            @RequestBody ExploreRequest request) {

        User user = getUser(authentication);

        // ❌ Unauthorized
        if (user == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(401)
                            .body((Object) Map.of("error", "Unauthorized")));
        }

        // 🔥 Convert String → Plan
        Plan plan = Plan.valueOf(user.getPlan().name().toUpperCase());

        // ❌ Expired plan
        boolean notExpired = plan == Plan.EXPLORER ||
                (user.getPlanExpiresAt() != null &&
                        user.getPlanExpiresAt().isAfter(OffsetDateTime.now()));

        if (!notExpired) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(403)
                            .body((Object) Map.of("error", "Plan expired. Please renew.")));
        }

        // 🔥 AI LIMIT CHECK
        int usedToday = exploreService.getTodayUsage(user.getId());

        if (!planService.canUseAI(plan, usedToday)) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(403)
                            .body((Object) Map.of("error", "AI limit reached. Upgrade your plan.")));
        }

        // 🚀 Async processing
        return CompletableFuture
                .supplyAsync(() -> exploreService.analyzeMarket(request, user.getId().toString()))
                .thenApply(res -> {
                    if (res != null && res.getConfidenceScore() > 0) {
                        exploreService.incrementUsage(user.getId());
                    }
                    return ResponseEntity.ok((Object) res);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500)
                            .body((Object) Map.of("error", "Something went wrong"));
                });
    }
}