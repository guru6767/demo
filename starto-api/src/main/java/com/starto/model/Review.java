package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review implements Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // who wrote the review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User reviewer;

    @Column(name = "reviewer_id", insertable = false, updatable = false)
    private UUID reviewerId;

    // who is being reviewed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User reviewed;

    @Column(name = "reviewed_id", insertable = false, updatable = false)
    private UUID reviewedId;

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}