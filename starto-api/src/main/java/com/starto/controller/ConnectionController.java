package com.starto.controller;

import com.starto.dto.ConnectionRequestDTO;
import com.starto.dto.ConnectionResponseDTO;
import com.starto.model.Connection;
import com.starto.model.User;
import com.starto.enums.Plan;
import com.starto.service.ConnectionService;
import com.starto.service.PlanService;
import com.starto.service.UserService;
import com.starto.service.WebSocketService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserService userService;
    private final WebSocketService webSocketService;
    private final PlanService planService;

    // 🔹 Helper method
    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return userService
                .getUserByFirebaseUid(authentication.getPrincipal().toString())
                .orElse(null);
    }

    // SEND REQUEST
    
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(
            Authentication authentication,
            @RequestBody ConnectionRequestDTO dto) {

        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        Connection connection = connectionService.sendRequest(
                user,
                dto.getReceiverId(),
                dto.getSignalId(),
                dto.getMessage()
        );

        webSocketService.send(
                "/topic/connections/" + connection.getReceiver().getId(),
                Map.of("type", "NEW_REQUEST", "data", connection)
        );

        return ResponseEntity.ok(ConnectionResponseDTO.from(connection));
    }

    // ACCEPT REQUEST
    @PutMapping("/{connectionId}/accept")
    public ResponseEntity<?> acceptRequest(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        Connection updated = connectionService.acceptRequest(user, connectionId);

        webSocketService.send(
                "/topic/connections/" + updated.getRequester().getId(),
                Map.of("type", "ACCEPT", "data", updated)
        );

        return ResponseEntity.ok(ConnectionResponseDTO.from(updated));
    }

    // REJECT REQUEST
    @PutMapping("/{connectionId}/reject")
    public ResponseEntity<?> rejectRequest(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        Connection updated = connectionService.rejectRequest(user, connectionId);

        webSocketService.send(
                "/topic/connections/" + updated.getRequester().getId(),
                Map.of("type", "REJECT", "data", updated)
        );

        return ResponseEntity.ok(ConnectionResponseDTO.from(updated));
    }

    // PENDING
    @GetMapping("/pending")
    public ResponseEntity<?> getPending(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
                connectionService.getPendingRequests(user.getId())
        );
    }

    // SENT
    @GetMapping("/sent")
    public ResponseEntity<?> getSent(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
                connectionService.getSentRequests(user.getId())
        );
    }

    // ACCEPTED
    @GetMapping("/accepted")
    public ResponseEntity<?> getAccepted(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
                connectionService.getAcceptedConnections(user.getId())
        );
    }

    //  WHATSAPP LINK (MONETIZATION POINT)
    @GetMapping("/{connectionId}/whatsapp")
    public ResponseEntity<?> getWhatsappLink(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        User user = getUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();
        Plan plan = user.getPlan();

        if (!planService.hasWhatsappAccess(plan)) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Upgrade your plan to unlock WhatsApp contact")
            );
        }

        Connection connection = connectionService.getConnectionById(connectionId);

        // Security check
        if (!connection.getRequester().getId().equals(user.getId()) &&
            !connection.getReceiver().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        String link = connectionService.getWhatsappLink(user, connectionId);

        return ResponseEntity.ok(Map.of("whatsappUrl", link));
    }
}