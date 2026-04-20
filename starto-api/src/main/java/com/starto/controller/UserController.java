package com.starto.controller;

import com.starto.model.User;
import com.starto.service.PresenceService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.OffsetDateTime;
import com.starto.service.PresenceService;

import com.starto.dto.PublicUserDTO;
import com.starto.enums.Plan;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PresenceService presenceService; 


    //checks username is available or not
  @GetMapping("/check-username")
public ResponseEntity<?> checkUsername(
        @RequestParam String username,
        @RequestParam String role) {

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


    //  Get my own full profile
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

// ← UPDATED heartbeat — both DB and Redis
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
