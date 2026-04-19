package com.starto.controller;

import com.starto.repository.NotificationRepository;
import com.starto.service.NotificationService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    // get all notifications
    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        notificationService.getNotifications(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // mark single notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            Authentication authentication,
            @PathVariable UUID id) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // mark all as read
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> {
                    notificationService.markAllAsRead(user.getId());
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        Map.of("count",
                                notificationService.countUnreadByUserId(user.getId()))))
                .orElse(ResponseEntity.status(401).build());
    }
}