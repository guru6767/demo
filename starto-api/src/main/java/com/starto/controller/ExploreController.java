package com.starto.controller;

import com.starto.dto.ExploreRequest;
import com.starto.dto.ExploreResponse;
import com.starto.service.ExploreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;

    // sends the request data
    @PostMapping("/analyze")
    public ResponseEntity<ExploreResponse> analyze(@RequestBody ExploreRequest request) {
        return ResponseEntity.ok(exploreService.analyzeMarket(request));
    }
}