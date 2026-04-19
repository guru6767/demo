package com.starto.dto;

import lombok.Data;
import lombok.Builder;
import com.starto.model.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDTO {

    private UUID id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String industry;
    private String subIndustry;
    private String city;
    private String state;
    private String country;
    private BigDecimal lat;
    private BigDecimal lng;
    private String bio;
    private String avatarUrl;
    private String websiteUrl;
    private String linkedinUrl;
    private String twitterUrl;
    private String githubUrl;
    private String plan;
    private OffsetDateTime planExpiresAt;
    private Integer signalCount;
    private Integer networkSize;
    private String networkTier;
    private Boolean isOnline;
    private OffsetDateTime lastSeen;
    private Boolean isVerified;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Mapper — converts entity to DTO
    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .industry(user.getIndustry())
                .subIndustry(user.getSubIndustry())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .lat(user.getLat())
                .lng(user.getLng())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .websiteUrl(user.getWebsiteUrl())
                .linkedinUrl(user.getLinkedinUrl())
                .twitterUrl(user.getTwitterUrl())
                .githubUrl(user.getGithubUrl())
                .plan(user.getPlan().name())
                .planExpiresAt(user.getPlanExpiresAt())
                .signalCount(user.getSignalCount())
                .networkSize(user.getNetworkSize())
                .networkTier(user.getNetworkTier())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}