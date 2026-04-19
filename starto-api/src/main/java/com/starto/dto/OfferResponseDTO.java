package com.starto.dto;

import com.starto.model.Offer;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class OfferResponseDTO {

    private UUID id;
    private UUID requesterId;
    private String requesterName;
    private String requesterAvatarUrl;
    private String requesterRole;

    private UUID receiverId;
    private String receiverName;
    private String receiverAvatarUrl;

    private String message;
    private String status;
    private OffsetDateTime createdAt;

    public static OfferResponseDTO from(Offer offer) {
        return OfferResponseDTO.builder()
                .id(offer.getId())
                .requesterId(offer.getRequester().getId())
                .requesterName(offer.getRequester().getName())
                .requesterAvatarUrl(offer.getRequester().getAvatarUrl())
                .requesterRole(offer.getRequester().getRole())
                .receiverId(offer.getReceiver().getId())
                .receiverName(offer.getReceiver().getName())
                .receiverAvatarUrl(offer.getReceiver().getAvatarUrl())
                .message(offer.getMessage())
                .status(offer.getStatus())
                .createdAt(offer.getCreatedAt())
                .build();
    }
}