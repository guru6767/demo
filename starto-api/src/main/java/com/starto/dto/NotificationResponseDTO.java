package com.starto.dto;

import com.starto.model.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponseDTO {

    private UUID id;
    private String type;
    private String message;
    private Boolean isRead;
    private OffsetDateTime createdAt;
    private UUID senderId;
    private String senderName;
    private String senderAvatarUrl;

    public static NotificationResponseDTO from(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())

                // FIXED
                .message(notification.getTitle() + " - " + notification.getBody())

                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())

                // FIXED
                .senderId(notification.getUser().getId())
                .senderName(notification.getUser().getName())
                .senderAvatarUrl(notification.getUser().getAvatarUrl())

                .build();
    }
}