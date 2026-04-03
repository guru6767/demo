package com.starto.controller;

import com.starto.model.User;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // checks username is available or not
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(
            @RequestParam String username,
            @RequestParam String role) {

        String finalUsername = username + "_" + role.toLowerCase();
        boolean available = userService.isUsernameAvailable(username, role);

        if (available) {
            return ResponseEntity.ok(Map.of(
                    "available", true,
                    "username", finalUsername));
        } else {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "Username already exists",
                    "username", finalUsername));
        }
    }

    // edit the user profile
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal String firebaseUid,
            @RequestBody User profileUpdates) {
        return userService.getUserByFirebaseUid(firebaseUid)
                .map(user -> {
                    // Update allowed fields
                    user.setName(profileUpdates.getName());
                    user.setUsername(profileUpdates.getUsername());
                    user.setBio(profileUpdates.getBio());
                    user.setIndustry(profileUpdates.getIndustry());
                    user.setCity(profileUpdates.getCity());
                    return ResponseEntity.ok(userService.updateProfile(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Frontend calls this every 30-60 seconds to keep user online
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        userService.markOnline(authentication.getPrincipal().toString());
        return ResponseEntity.ok().build();
    }

    // Frontend calls this on logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        userService.markOffline(authentication.getPrincipal().toString());
        return ResponseEntity.ok().build();
    }

    // Get my own full profile
    @GetMapping("/me")
    public ResponseEntity<User> getMe(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get any user by username (public)
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
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
}