package com.starto.controller;

import com.starto.model.User;
import com.starto.service.PresenceService;
import com.starto.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.OffsetDateTime;

import com.starto.dto.PublicUserDTO;
import com.starto.enums.Plan;

import java.util.Map;
import java.util.HashMap;

/**
 * User profile and presence controller.
 *
 * <p>Handles user profile read/write, username availability checks, plan status,
 * and online-presence heartbeats. Every authenticated session calls
 * {@link #getMe} on load and {@link #heartbeat} periodically.</p>
 */
@Tag(name = "Users", description = "Profile management, presence, and plan status")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PresenceService presenceService; 


    /**
     * Check whether a username+role combination is still available.
     *
     * <p>Public endpoint — no authentication required. Used during onboarding to
     * give instant feedback before form submission.</p>
     *
     * @param username base username (without role suffix)
     * @param role     user role (e.g. {@code founder}, {@code investor})
     * @return {@code {available: true|false, username: "resolved_username"}}
     */
    @Operation(summary = "Check username availability",
               description = "Returns whether username_role is taken. Public — no auth required.")
    @ApiResponse(responseCode = "200", description = "Availability result")
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(
            @Parameter(description = "Base username", required = true) @RequestParam String username,
            @Parameter(description = "User role",     required = true) @RequestParam String role) {

    String finalUsername = username + "_" + role.toLowerCase();
    boolean available = userService.isUsernameAvailable(username, role);

    if (available) {
        return ResponseEntity.ok(Map.of(
            "available", true,
            "username", finalUsername
        ));
    } else {
        return ResponseEntity.ok(Map.of(
            "available", false,
            "message", "Username already exists",
            "username", finalUsername
        ));
    }
}


// edit the user profile
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal String firebaseUid,
            @RequestBody User profileUpdates) {
        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    user.setName(profileUpdates.getName());
                user.setUsername(profileUpdates.getUsername());
                user.setBio(profileUpdates.getBio());
                user.setIndustry(profileUpdates.getIndustry());
                user.setSubIndustry(profileUpdates.getSubIndustry());
                user.setCity(profileUpdates.getCity());
                user.setState(profileUpdates.getState());
                user.setCountry(profileUpdates.getCountry());
                user.setAvatarUrl(profileUpdates.getAvatarUrl());
                user.setWebsiteUrl(profileUpdates.getWebsiteUrl());
                user.setLinkedinUrl(profileUpdates.getLinkedinUrl());
                user.setTwitterUrl(profileUpdates.getTwitterUrl());
                user.setGithubUrl(profileUpdates.getGithubUrl());
                user.setLat(profileUpdates.getLat());
                user.setLng(profileUpdates.getLng());
                user.setFcmToken(profileUpdates.getFcmToken());
                    return ResponseEntity.ok(userService.updateProfile(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Get the authenticated user's own full profile.
     *
     * <p>Called on every app load to hydrate client state. Also updates the user's
     * online status and downgrades expired plans inline.</p>
     *
     * @return the full {@link User} entity for the caller
     */
    @Operation(summary = "Get my profile",
               description = "Returns the full profile for the authenticated user. Updates online status.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found (not yet registered)")
    })
    @GetMapping("/me")
    public ResponseEntity<User> getMe(Authentication authentication) {
    if (authentication == null) return ResponseEntity.status(401).build();
    return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}


//  Get any user by username (public)
@GetMapping("/{username}")
public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
    return userService.getUserByUsername(username)
            .map(user -> ResponseEntity.ok(PublicUserDTO.from(user)))
            .orElse(ResponseEntity.notFound().build());
}

@GetMapping("/{username}/online-status")
public ResponseEntity<Map<String, Object>> getOnlineStatus(@PathVariable String username) {
    return userService.getUserByUsername(username)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("username", user.getUsername());
                response.put("isOnline", user.getIsOnline());
                response.put("lastSeen", user.getLastSeen().toString());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
}


@GetMapping("/plan-status")
public ResponseEntity<?> getPlanStatus(Authentication authentication) {
    if (authentication == null) return ResponseEntity.status(401).build();

    return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
            .map(user -> {
                Map<String, Object> status = new HashMap<>();
                status.put("plan", user.getPlan().name());
                status.put("planExpiresAt", user.getPlanExpiresAt() != null 
    ? user.getPlanExpiresAt().toString() 
    : null);
                status.put("isActive", user.getPlanExpiresAt() == null || 
                           user.getPlanExpiresAt().isAfter(OffsetDateTime.now()));

                // days remaining
                if (user.getPlanExpiresAt() != null) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                        OffsetDateTime.now(), user.getPlanExpiresAt()
                    );
                    status.put("daysLeft", Math.max(daysLeft, 0));
                } else {
                    status.put("daysLeft", user.getPlan() == Plan.EXPLORER ? "unlimited" : 0);
                }

                return ResponseEntity.ok(status);
            })
            .orElse(ResponseEntity.status(401).build());
}

    /**
     * Send a presence heartbeat (called every ~30 s by the client).
     *
     * <p>Updates both the PostgreSQL {@code users.last_seen} column and the
     * Redis presence cache used for real-time online indicators.</p>
     *
     * @return {@code 200 OK} with no body
     */
    @Operation(summary = "Presence heartbeat",
               description = "Mark the caller as online. Should be called every 30 s by active clients.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Presence updated")
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();

        String uid = authentication.getPrincipal().toString();

        // update DB
        userService.updatePresence(uid);

        // update Redis — get city from user for presence topic
        userService.getUserByFirebaseUid(uid).ifPresent(user -> {
            String city = user.getCity() != null ? user.getCity() : "unknown";
            presenceService.markOnline(uid, city);
        });

        return ResponseEntity.ok().build();
    }

    // ← UPDATED logout — both DB and Redis
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();

        String uid = authentication.getPrincipal().toString();

        // update DB
        userService.markOffline(uid);

        // update Redis
        presenceService.markOffline(uid);

        return ResponseEntity.ok().build();
    }

}
