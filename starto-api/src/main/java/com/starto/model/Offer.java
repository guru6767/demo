package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_offers_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_offers_requester_id", columnList = "requester_id"),
        @Index(name = "idx_offers_signal_id", columnList = "signal_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "requester_id", insertable = false, updatable = false)
    private UUID requesterId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "receiver_id", insertable = false, updatable = false)
    private UUID receiverId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id", nullable = false)
    private Signal signal;

    @Column(name = "signal_id", insertable = false, updatable = false)
    private UUID signalId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "portfolio_link")
    private String portfolioLink;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}