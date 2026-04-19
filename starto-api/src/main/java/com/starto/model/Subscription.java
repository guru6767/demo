package com.starto.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String plan; // matches Plan enum name

    @Column(nullable = false)
    private Integer amountPaid; // in paise (₹149 = 14900)

    @Column
    private String razorpayOrderId;

    @Column
    private String razorpayPaymentId;

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, EXPIRED, FAILED

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column
    private String razorpaySubscriptionId;
}