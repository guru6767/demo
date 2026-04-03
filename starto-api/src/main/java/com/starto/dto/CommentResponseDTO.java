package com.starto.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CommentResponseDTO {
    private UUID id;
    private UUID signalId;
    private UUID userId;
    private String username;
    private String content;
    private UUID parentId;
    private OffsetDateTime createdAt;
    private List<CommentResponseDTO> replies;
}