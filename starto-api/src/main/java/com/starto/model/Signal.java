package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Formula;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "signals", indexes = {
        @Index(name = "idx_signals_user_id", columnList = "user_id"),
        @Index(name = "idx_signals_status", columnList = "status"),
        @Index(name = "idx_signals_city", columnList = "city"),
        @Index(name = "idx_signals_seeking", columnList = "seeking"),
        @Index(name = "idx_signals_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signal implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @Formula("(SELECT u.username FROM users u WHERE u.id = user_id)")
    private String username;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 50, nullable = false)
    private String stage;

    @Column(length = 100, nullable = false)
    private String city;

    @Column(length = 100, nullable = false)
    private String state;

    private BigDecimal lat;
    private BigDecimal lng;

    @Column(name = "timeline_days")
    private Integer timelineDays;

    @Column(length = 100)
    private String compensation;

    @Column(length = 20)
    @Builder.Default
    private String visibility = "global";

    @Column(length = 20)
    @Builder.Default
    private String status = "open";

    @Column(name = "signal_strength", length = 20)
    @Builder.Default
    private String signalStrength = "normal";

    @Column(name = "response_count")
    @Builder.Default
    private Integer responseCount = 0;

    @Column(name = "offer_count")
    @Builder.Default
    private Integer offerCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "is_boosted")
    @Builder.Default
    private Boolean isBoosted = false;

    @Column(name = "boost_expires_at")
    private OffsetDateTime boostExpiresAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(length = 50)
    private String seeking;

    // public UUID getId() {
    // return id;
    // }

    // public void setId(UUID id) {
    // this.id = id;
    // }

    // public User getUser() {
    // return user;
    // }

    // public void setUser(User user) {
    // this.user = user;
    // }

    // public String getType() {
    // return type;
    // }

    // public void setType(String type) {
    // this.type = type;
    // }

    // public String getTitle() {
    // return title;
    // }

    // public void setTitle(String title) {
    // this.title = title;
    // }

    // public String getDescription() {
    // return description;
    // }

    // public void setDescription(String description) {
    // this.description = description;
    // }

    // public String getStatus() {
    // return status;
    // }

    // public void setStatus(String status) {
    // this.status = status;
    // }

    // public OffsetDateTime getExpiresAt() {
    // return expiresAt;
    // }

    // public void setExpiresAt(OffsetDateTime expiresAt) {
    // this.expiresAt = expiresAt;
    // }
}