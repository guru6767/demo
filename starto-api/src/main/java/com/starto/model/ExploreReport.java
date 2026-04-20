package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "explore_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExploreReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String location;
    private String industry;
    private Long budget;
    private String stage;

    @Column(name = "target_customer")
    private String targetCustomer;

    @Column(name = "report_data", nullable = false, columnDefinition = "jsonb")
    private String reportData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    
}
