package com.starto.service;

import com.starto.model.Notification;
import com.starto.model.User;
import com.starto.repository.NotificationRepository;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.starto.service.FcmService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final FcmService fcmService;

    @Transactional
    public Notification send(UUID userId, String type, String title, String body, Map<String, Object> data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .data(data)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // send real-time via WebSocket
        webSocketService.send("/topic/notifications/" + userId, saved);

        fcmService.sendPush(user.getFcmToken(), title, body);

        return saved;
    }

    public List<Notification> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    // NotificationService.java — replace with single bulk update
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    // NotificationService.java
    public long countUnreadByUserId(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
}