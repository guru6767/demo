package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "firebase_uid", unique = true, nullable = false, length = 128)
    private String firebaseUid;

    @Column(unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(length = 100)
    private String industry;

    @Column(name = "sub_industry", length = 100)
    private String subIndustry;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    @Builder.Default
    private String country = "India";

    private BigDecimal lat;
    private BigDecimal lng;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    @Column(name = "linkedin_url", columnDefinition = "TEXT")
    private String linkedinUrl;

    // add these two
    @Column(name = "twitter_url", columnDefinition = "TEXT")
    private String twitterUrl;

    @Column(name = "github_url", columnDefinition = "TEXT")
    private String githubUrl;

    @Column(length = 20)
    @Builder.Default
    private String plan = "free";

    @Column(name = "plan_expires_at")
    private OffsetDateTime planExpiresAt;

    @Column(name = "signal_count")
    @Builder.Default
    private Integer signalCount = 0;

    @Column(name = "network_size")
    @Builder.Default
    private Integer networkSize = 0;

    @Column(name = "network_tier", length = 20)
    @Builder.Default
    private String networkTier = "local";

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    @Column(name = "last_seen")
    @Builder.Default
    private OffsetDateTime lastSeen = OffsetDateTime.now();

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // public UUID getId() {
    // return id;
    // }

    // public void setId(UUID id) {
    // this.id = id;
    // }

    // public String getFirebaseUid() {
    // return firebaseUid;
    // }

    // public void setFirebaseUid(String firebaseUid) {
    // this.firebaseUid = firebaseUid;
    // }

    // public String getUsername() {
    // return username;
    // }

    // public void setUsername(String username) {
    // this.username = username;
    // }

    // public String getName() {
    // return name;
    // }

    // public void setName(String name) {
    // this.name = name;
    // }

    // public String getEmail() {
    // return email;
    // }

    // public void setEmail(String email) {
    // this.email = email;
    // }

    // public String getBio() {
    // return bio;
    // }

    // public void setBio(String bio) {
    // this.bio = bio;
    // }

    // public String getIndustry() {
    // return industry;
    // }

    // public void setIndustry(String industry) {
    // this.industry = industry;
    // }

    // public String getCity() {
    // return city;
    // }

    // public void setCity(String city) {
    // this.city = city;
    // }

    // public void setLastSeen(OffsetDateTime lastSeen) {
    // this.lastSeen = lastSeen;
    // }

    // public void setIsOnline(Boolean isOnline) {
    // this.isOnline = isOnline;
    // }

    // public void setUpdatedAt(OffsetDateTime updatedAt) {
    // this.updatedAt = updatedAt;
    // }
}