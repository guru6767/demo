package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nearby_spaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySpace implements Serializable{
        private static final long serialVersionUID = 1L;
        
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
@JoinColumn(name = "user_id", nullable = false)
private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private BigDecimal lat;

    @Column(nullable = false)
    private BigDecimal lng;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String contact;
    private String website;
    @Builder.Default
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
