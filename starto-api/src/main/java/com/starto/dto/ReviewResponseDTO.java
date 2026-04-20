package com.starto.dto;

import com.starto.model.Review;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponseDTO {

    private UUID id;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;

    // reviewer info
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatarUrl;

    // reviewed user
    private UUID reviewedUserId;

    public static ReviewResponseDTO from(Review review) {
    return ReviewResponseDTO.builder()
            .id(review.getId())
            .rating(review.getRating())
            .comment(review.getComment())
            .createdAt(review.getCreatedAt())
            .reviewerId(review.getReviewer().getId())
            .reviewerName(review.getReviewer().getName())
            .reviewerAvatarUrl(review.getReviewer().getAvatarUrl())
            .reviewedUserId(review.getReviewed().getId()) // ✅ FIXED
            .build();
}
}