package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "connections", indexes = {
        @Index(name = "idx_connections_requester", columnList = "requester_id"),
        @Index(name = "idx_connections_receiver", columnList = "receiver_id"),
        @Index(name = "idx_connections_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id", nullable = true)
    private Signal signal;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // derived getters — no extra DB columns needed
    public UUID getRequesterId() {
        return requester != null ? requester.getId() : null;
    }

    public UUID getReceiverId() {
        return receiver != null ? receiver.getId() : null;
    }

    public UUID getSignalId() {
        return signal != null ? signal.getId() : null;
    }
}