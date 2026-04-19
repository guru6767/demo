package com.starto.controller;

import com.starto.model.Review;
import com.starto.service.ReviewService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    @PostMapping("/{userId}")
    public ResponseEntity<?> addReview(
            Authentication authentication,
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> body) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        int rating = (int) body.get("rating");
        String comment = (String) body.getOrDefault("comment", "");

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> {
                    try {
                        Review review = reviewService.addReview(user, userId, rating, comment);
                        return ResponseEntity.ok(review);
                    } catch (RuntimeException ex) {
                        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getReviews(@PathVariable UUID userId) {
        return ResponseEntity.ok(reviewService.getReviews(userId));
    }

    @GetMapping("/{userId}/summary")
    public ResponseEntity<?> getSummary(@PathVariable UUID userId) {
        return ResponseEntity.ok(reviewService.getReviewSummary(userId));
    }
}