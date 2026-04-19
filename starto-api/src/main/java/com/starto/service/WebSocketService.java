package com.starto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Generic sender (BEST APPROACH)
    public void send(String destination, Object data) {
        messagingTemplate.convertAndSend(destination, data);
    }
}