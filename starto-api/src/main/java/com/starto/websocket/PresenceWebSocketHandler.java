package com.starto.websocket;

import com.starto.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PresenceWebSocketHandler {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/presence.heartbeat")
    public void handleHeartbeat(@AuthenticationPrincipal String firebaseUid, @Payload Map<String, String> payload) {
        String city = payload.get("city");
        presenceService.markOnline(firebaseUid, city);

        // Broadcast to city topic
        messagingTemplate.convertAndSend("/topic/presence/" + city,
                Map.of("userId", firebaseUid, "status", "online"));
    }
}
