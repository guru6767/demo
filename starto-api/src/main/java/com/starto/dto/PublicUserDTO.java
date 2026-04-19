package com.starto.dto;

import com.starto.model.User;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PublicUserDTO {
    private UUID id;
    private String username;
    private String name;
    private String role;
    private String industry;
    private String subIndustry;
    private String city;
    private String state;
    private String country;
    private String bio;
    private String avatarUrl;
    private String websiteUrl;
    private String linkedinUrl;
    private String twitterUrl;
    private String githubUrl;
    private String plan;
    private Boolean isOnline;
    private Boolean isVerified;
    private Integer signalCount;
    private Integer networkSize;
    private String networkTier;

    // no email, no phone, no fcmToken, no firebaseUid

    public static PublicUserDTO from(User user) {
        return PublicUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .industry(user.getIndustry())
                .subIndustry(user.getSubIndustry())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .websiteUrl(user.getWebsiteUrl())
                .linkedinUrl(user.getLinkedinUrl())
                .twitterUrl(user.getTwitterUrl())
                .githubUrl(user.getGithubUrl())
                .plan(user.getPlan().name())
                .isOnline(user.getIsOnline())
                .isVerified(user.getIsVerified())
                .signalCount(user.getSignalCount())
                .networkSize(user.getNetworkSize())
                .networkTier(user.getNetworkTier())
                .build();
    }
}