package com.starto.controller;

import com.starto.dto.ConnectionRequestDTO;
import com.starto.model.Connection;
import com.starto.service.ConnectionService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserService userService;

    // talent sends request
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(
            Authentication authentication,
            @RequestBody ConnectionRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        connectionService.sendRequest(user, dto.getSignalId(), dto.getMessage())))
                .orElse(ResponseEntity.status(401).build());
    }

    // founder accepts
    @PutMapping("/{connectionId}/accept")
    public ResponseEntity<?> acceptRequest(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(connectionService.acceptRequest(user, connectionId)))
                .orElse(ResponseEntity.status(401).build());
    }

    // founder rejects
    @PutMapping("/{connectionId}/reject")
    public ResponseEntity<?> rejectRequest(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(connectionService.rejectRequest(user, connectionId)))
                .orElse(ResponseEntity.status(401).build());
    }

    // founder sees pending requests
    @GetMapping("/pending")
    public ResponseEntity<List<Connection>> getPending(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(connectionService.getPendingRequests(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // talent sees sent requests
    @GetMapping("/sent")
    public ResponseEntity<List<Connection>> getSent(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(connectionService.getSentRequests(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // get accepted connections
    @GetMapping("/accepted")
    public ResponseEntity<List<Connection>> getAccepted(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(connectionService.getAcceptedConnections(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // get whatsapp link after acceptance
    @GetMapping("/{connectionId}/whatsapp")
    public ResponseEntity<?> getWhatsappLink(
            Authentication authentication,
            @PathVariable UUID connectionId) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> {
                    Connection connection = connectionService.getConnectionById(connectionId);

                    // only founder (receiver) can access whatsapp link
                    if (connection.getReceiverId().equals(user.getId())) {
                        String link = connectionService.getWhatsappLink(user, connectionId);
                        return ResponseEntity.ok(Map.of("whatsappUrl", link));
                    }

                    // talent (requester) can only access if premium
                    if (connection.getRequesterId().equals(user.getId())) {
                        if (user.getPlan() != null && user.getPlan().equalsIgnoreCase("premium")) {
                            String link = connectionService.getWhatsappLink(user, connectionId);
                            return ResponseEntity.ok(Map.of("whatsappUrl", link));
                        } else {
                            return ResponseEntity.status(403).body(Map.of(
                                    "error", "Upgrade to premium to initiate contact",
                                    "upgradeUrl", "/api/subscriptions/upgrade"));
                        }
                    }

                    return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}