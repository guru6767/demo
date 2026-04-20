package com.starto.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ai_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
},          indexes = {
        @Index(name = "idx_ai_usage_user_date", columnList = "user_id, date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int usedCount;
}