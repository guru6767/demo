package com.starto.controller;

import com.starto.model.NearbySpace;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.service.SignalService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import com.starto.service.WebSocketService;
import com.starto.dto.SignalRequestDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;
    private final UserService userService;
    private final WebSocketService webSocketService;

    // create the signal
    @PostMapping
    public ResponseEntity<?> createSignal(
            Authentication authentication,
            @RequestBody SignalRequestDTO dto) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();

        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    try {
                        signalService.validateSignalCreation(user);
                    } catch (RuntimeException ex) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", ex.getMessage(),
                                "upgradeUrl", "/api/subscriptions/upgrade"));
                    }

                    // map DTO to Signal
                    Signal signal = Signal.builder()
                            .type(dto.getType())
                            .title(dto.getTitle())
                            .description(dto.getDescription())
                            .stage(dto.getStage())
                            .city(dto.getCity())
                            .state(dto.getState())
                            .category(dto.getCategory())
                            .seeking(dto.getSeeking())
                            .compensation(dto.getCompensation())
                            .visibility(dto.getVisibility() != null ? dto.getVisibility() : "global")
                            .signalStrength(dto.getSignalStrength() != null ? dto.getSignalStrength() : "normal")
                            .timelineDays(dto.getTimelineDays())
                            .lat(dto.getLat())
                            .lng(dto.getLng())
                            .user(user)
                            .build();

                    signal.setUser(user);

                    // SAVE
                    Signal saved = signalService.createSignal(signal);

                    // WEBSOCKET
                    webSocketService.send("/topic/signals", saved);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // Get all the signals based on filter
    @GetMapping
    public ResponseEntity<List<Signal>> getSignals(@RequestParam(required = false) String city,
            @RequestParam(required = false) String seeking,
            @RequestParam(required = false) String username) {

        // username + seeking
        if (username != null && seeking != null) {
            return ResponseEntity.ok(signalService.getSignalsByUsernameAndSeeking(username, seeking));
        }

        // username only
        if (username != null) {
            return ResponseEntity.ok(signalService.searchSignalsByUsername(username));
        }

        // seeking + city
        if (seeking != null && city != null) {
            return ResponseEntity.ok(signalService.getSignalsBySeekingAndCity(seeking, city));
        }

        // seeking only
        if (seeking != null) {
            return ResponseEntity.ok(signalService.getSignalsBySeeking(seeking));
        }

        // city only
        if (city != null) {
            return ResponseEntity.ok(signalService.getSignalsByCity(city));
        }
        return ResponseEntity.ok(signalService.getActiveSignals());
    }

    // get the signal based on ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable UUID id, Authentication authentication) {

        UUID viewerUserId = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            String firebaseUid = authentication.getPrincipal().toString();
            viewerUserId = userService.getUserByFirebaseUid(firebaseUid)
                    .map(User::getId)
                    .orElse(null);
        }

        // Try Signal
        Signal signal = signalService.getSignalByIdSafe(id);
        if (signal != null) {
            signalService.trackView(id, viewerUserId);
            return ResponseEntity.ok(signal);
        }

        // Try Space
        NearbySpace space = signalService.getNearbySpaceById(id);
        if (space != null) {
            return ResponseEntity.ok(space);
        }

        return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
    }

    // get all my signals
    @GetMapping("/mine")
    public ResponseEntity<?> getMyData(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        String firebaseUid = authentication.getPrincipal().toString();

        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    List<Signal> signals = signalService.getSignalsByUser(user.getId());
                    return ResponseEntity.ok(signals);
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // edit the signal
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSignal(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody Signal updatedSignal) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();

        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    try {
                        Object updated = signalService.updatePost(id, user, updatedSignal);

                        // broadcast based on type
                        if (updated instanceof Signal) {
                            webSocketService.send("/topic/signals",
                                    Map.of("type", "UPDATE", "data", updated));
                        } else {
                            webSocketService.send("/topic/spaces", updated);
                        }

                        return ResponseEntity.ok(updated);

                    } catch (RuntimeException ex) {
                        return ResponseEntity.status(403).body(ex.getMessage());
                    }
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // delete the signal
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(Authentication authentication, @PathVariable UUID id) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();

        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    try {
                        String result = signalService.deletePost(id, user);

                        if (result.startsWith("Signal")) {
                            webSocketService.send("/topic/signals",
                                    Map.of("type", "DELETE", "signalId", id));
                        } else {
                            webSocketService.send("/topic/spaces",
                                    Map.of("type", "DELETE", "spaceId", id));
                        }

                        return ResponseEntity.ok(result);

                    } catch (RuntimeException ex) {
                        return ResponseEntity.status(403).body(ex.getMessage());
                    }
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // Add insights endpoint — owner only
    @GetMapping("/{id}/insights")
    public ResponseEntity<?> getInsights(
            Authentication authentication,
            @PathVariable UUID id) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();

        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    Signal signal = signalService.getSignalById(id);

                    // only owner can see insights
                    if (!signal.getUserId().equals(user.getId())) {
                        return ResponseEntity.status(403).body("Forbidden");
                    }

                    return ResponseEntity.ok(signalService.getInsights(id));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // getting the nearBy data
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyMapData(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "10") double radiusKm) {

        if (radiusKm <= 0) {
            radiusKm = 10;
        }

        List<Signal> nearbySignals = signalService.getNearbySignals(lat, lng, radiusKm);
        List<NearbySpace> nearbySpaces = signalService.getNearbySpaces(lat, lng, radiusKm);

        return ResponseEntity.ok(Map.of(
                "latitude", lat,
                "longitude", lng,
                "radiusKm", radiusKm,
                "signals", nearbySignals,
                "nearbySpaces", nearbySpaces));
    }

    @PostMapping("/spaces")
    public ResponseEntity<?> createNearbySpace(
            @RequestBody NearbySpace nearbySpace) {
        try {
            NearbySpace created = signalService.createNearbySpace(nearbySpace);

            // broadcast new space
            webSocketService.send("/topic/spaces", created);

            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/spaces")
    public ResponseEntity<List<NearbySpace>> getNearbySpaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(signalService.getNearbySpaces(lat, lng, radiusKm));
    }
}