package com.starto.controller;

import com.starto.model.NearbySpace;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.service.SignalService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import com.starto.service.WebSocketService;
import com.starto.dto.SignalRequestDTO;
import com.starto.dto.NearbyUserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Signal management controller — the platform's most-used API surface.
 *
 * <p>Signals are time-boxed opportunity posts (funding asks, co-founder searches,
 * mentor requests) that expire after 7 days by default. Supports full CRUD,
 * nearby geo-lookup, real-time WebSocket broadcast, and per-signal analytics.</p>
 */
@Tag(name = "Signals", description = "Create, read, update, delete and discover opportunity signals")
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;
    private final UserService userService;
    private final WebSocketService webSocketService;

    /**
     * Create a new signal.
     *
     * <p>Validates the caller's subscription plan limits before persisting.
     * On success, broadcasts the new signal over WebSocket {@code /topic/signals}.</p>
     *
     * @param dto validated signal payload
     * @return {@code 200 OK} with the saved {@link Signal}, {@code 403} if plan limit exceeded
     */
    @Operation(summary = "Create a signal",
               description = "Posts a new opportunity signal. Requires a valid Firebase ID token. Plan limits apply.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Signal created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid Firebase token"),
        @ApiResponse(responseCode = "403", description = "Plan limit reached — upgrade required")
    })
    @PostMapping
    public ResponseEntity<?> createSignal(
            Authentication authentication,
            @Valid @RequestBody SignalRequestDTO dto) {

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
                            "upgradeUrl", "/api/subscriptions/upgrade"
                    ));
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

                //  SAVE
                Signal saved = signalService.createSignal(signal);

                //  WEBSOCKET
                webSocketService.send("/topic/signals", saved);

                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.status(401).build());
}
        
   
    /**
     * List active signals with optional filters.
     *
     * @param city     filter by city name
     * @param seeking  filter by seeking type (e.g. "funding", "cofounder")
     * @param username filter by signal owner's username
     * @return list of matching {@link Signal} objects
     */
    @Operation(summary = "List signals",
               description = "Returns active signals. Supports optional ?city, ?seeking, ?username filters.")
    @ApiResponse(responseCode = "200", description = "Signal list")
    @GetMapping
    public ResponseEntity<List<Signal>> getSignals(
            @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
            @Parameter(description = "Filter by what the poster is seeking") @RequestParam(required = false) String seeking,
            @Parameter(description = "Filter by owner username") @RequestParam(required = false) String username) {

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


    //get the signal based on ID
   @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable UUID id, Authentication authentication) {

        UUID viewerUserId = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            String firebaseUid = authentication.getPrincipal().toString();
            viewerUserId = userService.getUserByFirebaseUid(firebaseUid)
                    .map(User::getId)
                    .orElse(null);
        }

        //  Try Signal
        Signal signal = signalService.getSignalByIdSafe(id);
        if (signal != null) {
            signalService.trackView(id, viewerUserId);
            return ResponseEntity.ok(signal);
        }

        //  Try Space
        NearbySpace space = signalService.getNearbySpaceById(id);
        if (space != null) {
            return ResponseEntity.ok(space);
        }

        return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
    }

      //get all my signals
 @GetMapping("/my")
public ResponseEntity<?> getMyData(
        Authentication authentication,
        @RequestParam(required = false) String category) {

    if (authentication == null || authentication.getPrincipal() == null) {
        return ResponseEntity.status(401).build();
    }

    String firebaseUid = authentication.getPrincipal().toString();

    return userService.getUserByFirebaseUid(firebaseUid)
            .map(user -> {

                List<Signal> signals;

                //  FILTER LOGIC
                if (category != null) {
                    signals = signalService.getSignalsByUserAndCategory(
                            user.getId(), category.toLowerCase()
                    );
                } else {
                    signals = signalService.getSignalsByUser(user.getId());
                }

                List<NearbySpace> spaces = signalService.getSpacesByUser(user.getId());

                return ResponseEntity.ok(Map.of(
                        "signals", signals,
                        "spaces", spaces
                ));
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

                    //  broadcast based on type
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

    /**
     * Discover nearby signals, spaces, and users within {@code radiusKm} km.
     *
     * @param lat      latitude of the search centre
     * @param lng      longitude of the search centre
     * @param radiusKm search radius in kilometres (default 10)
     * @return map containing {@code signals}, {@code nearbySpaces}, and {@code users}
     */
    @Operation(summary = "Nearby map data",
               description = "Returns signals, coworking spaces and users within radiusKm of the given coordinates.")
    @ApiResponse(responseCode = "200", description = "Nearby items")
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyMapData(
            @Parameter(description = "Centre latitude",  required = true) @RequestParam double lat,
            @Parameter(description = "Centre longitude", required = true) @RequestParam double lng,
            @Parameter(description = "Radius in km — default 10") @RequestParam(required = false, defaultValue = "10") double radiusKm) {

        if (radiusKm <= 0) {
            radiusKm = 10;
        }

        List<Signal> nearbySignals = signalService.getNearbySignals(lat, lng, radiusKm);
        List<NearbySpace> nearbySpaces = signalService.getNearbySpaces(lat, lng, radiusKm);
        List<NearbyUserDTO> nearbyUsers = signalService.getNearbyUsers(lat, lng, radiusKm);

        return ResponseEntity.ok(Map.of(
                "latitude", lat,
                "longitude", lng,
                "radiusKm", radiusKm,
                "signals", nearbySignals,
                "nearbySpaces", nearbySpaces,
                "users", nearbyUsers
        ));
    }

    @PostMapping("/spaces")
    public ResponseEntity<?> createNearbySpace(
            @RequestBody NearbySpace nearbySpace) {
        try {
           NearbySpace created = signalService.createNearbySpace(nearbySpace);

//  broadcast new space
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

