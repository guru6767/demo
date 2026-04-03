package com.starto.controller;

import com.starto.model.User;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import com.starto.service.PasswordResetService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    // saving the user details in DB
    @PostMapping("/register")
    public ResponseEntity<?> register(
            Authentication authentication,
            @RequestBody User userDetails) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized: Missing or invalid Firebase token");
        }

        String firebaseUid = authentication.getPrincipal().toString();

        User user = userService.createOrUpdateUser(
                firebaseUid,
                userDetails.getEmail(),
                userDetails.getName(),
                userDetails.getUsername(),
                userDetails.getRole());
        return ResponseEntity.ok(user);
    }

    // Getting the userDetails
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized: Missing or invalid Firebase token");
        }

        // Your UID is stored as principal in the filter
        String firebaseUid = authentication.getPrincipal().toString();
        System.out.println("checking the /me mapping");
        return userService.getUserByFirebaseUid(firebaseUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        passwordResetService.sendPasswordResetEmail(email);
        return ResponseEntity.ok(Map.of("message", "If this email is registered, a reset link has been sent."));
    }

    // Resetting the password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters");
        }
        String firebaseUid = authentication.getPrincipal().toString();
        passwordResetService.updatePassword(firebaseUid, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

}