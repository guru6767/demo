package com.starto.controller;

import com.starto.model.NearbySpace;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.service.SignalService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;
    private final UserService userService;

    // creatint the signal
    @PostMapping
    public ResponseEntity<?> createSignal(
            Authentication authentication,
            @RequestBody Signal signal) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();

        // Normalize: frontend may send dev_ or local- prefixed tokens
        if (firebaseUid.startsWith("local-")) {
            firebaseUid = "dev_" + firebaseUid.substring(6);
        }

        Optional<User> userOpt = userService.getUserByFirebaseUid(firebaseUid);
        
        // If user doesn't exist but it's a dev UID, auto-create
        if (userOpt.isEmpty() && firebaseUid.startsWith("dev_")) {
            String devUsername = firebaseUid.substring(4);
            userOpt = Optional.of(userService.createOrUpdateUser(
                firebaseUid, 
                devUsername + "@example.com", 
                devUsername, 
                devUsername, 
                "Founder"
            ));
        }

        return userOpt.map(user -> {
                    try {
                        signalService.validateSignalCreation(user);
                    } catch (RuntimeException ex) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", ex.getMessage(),
                                "upgradeUrl", "/api/subscriptions/upgrade"));
                    }
                    signal.setUser(user);
                    return ResponseEntity.ok(signalService.createSignal(signal));
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
            return ResponseEntity.ok(signalService.getSignalsByUsername(username));
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
    public ResponseEntity<Signal> getSignal(
            @PathVariable UUID id,
            Authentication authentication) {

        // track viewer — null if not logged in
        UUID viewerUserId = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            String firebaseUid = authentication.getPrincipal().toString();
            viewerUserId = userService.getUserByFirebaseUid(firebaseUid)
                    .map(User::getId)
                    .orElse(null);
        }

        signalService.trackView(id, viewerUserId);
        return ResponseEntity.ok(signalService.getSignalById(id));
    }

    // get all my signals
    @GetMapping("/my")
    public ResponseEntity<List<Signal>> getMySignals(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String firebaseUid = authentication.getPrincipal().toString();
        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> ResponseEntity.ok(signalService.getSignalsByUser(user.getId())))
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
                    Signal existing = signalService.getSignalById(id);

                    // ✅ Only the owner can edit
                    if (!existing.getUserId().equals(user.getId())) {
                        return ResponseEntity.status(403).body("Forbidden: You don't own this signal");
                    }

                    existing.setType(updatedSignal.getType());
                    existing.setSeeking(updatedSignal.getSeeking());
                    existing.setCategory(updatedSignal.getCategory());
                    existing.setTitle(updatedSignal.getTitle());
                    existing.setDescription(updatedSignal.getDescription());
                    existing.setStage(updatedSignal.getStage());
                    existing.setCity(updatedSignal.getCity());
                    existing.setState(updatedSignal.getState());
                    existing.setLat(updatedSignal.getLat());
                    existing.setLng(updatedSignal.getLng());
                    existing.setTimelineDays(updatedSignal.getTimelineDays());
                    existing.setCompensation(updatedSignal.getCompensation());
                    existing.setVisibility(updatedSignal.getVisibility());
                    existing.setSignalStrength(updatedSignal.getSignalStrength());

                    return ResponseEntity.ok(signalService.createSignal(existing));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    // delete the signal
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSignal(
            Authentication authentication,
            @PathVariable UUID id) {

        if (authentication == null || authentication.getPrincipal() == null)
            return ResponseEntity.status(401).build();

        String firebaseUid = authentication.getPrincipal().toString();
        // Normalize local- to dev_
        if (firebaseUid.startsWith("local-")) {
            firebaseUid = "dev_" + firebaseUid.substring(6);
        }

        final String uid = firebaseUid;
        return userService.getUserByFirebaseUid(uid)
                .map(user -> {
                    Signal existing = signalService.getSignalById(id);
                    if (existing == null) return ResponseEntity.status(404).body("Signal not found");

                    // ✅ Only the owner can delete
                    if (!existing.getUserId().equals(user.getId())) {
                        return ResponseEntity.status(403).body("Forbidden: You don't own this signal");
                    }

                    signalService.deleteSignal(id);
                    return ResponseEntity.ok().body("Signal deleted successfully");
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