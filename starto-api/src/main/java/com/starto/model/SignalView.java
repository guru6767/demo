package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "signal_views", indexes = {
        @Index(name = "idx_signal_views_signal_id", columnList = "signal_id"),
        @Index(name = "idx_signal_views_viewer", columnList = "viewer_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "signal_id", nullable = false)
    private UUID signalId;

    @Column(name = "viewer_user_id")
    private UUID viewerUserId; // null if anonymous

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_follower")
    private Boolean isFollower;
}