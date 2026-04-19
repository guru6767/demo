package com.starto.model;

import com.starto.enums.Plan;
import com.starto.enums.BillingType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Plan code;

    private String name;

    private int pricePaise;

    private int durationDays;

    @Enumerated(EnumType.STRING)
    private BillingType billingType; // ONE_TIME / RECURRING

    private String razorpayPlanId; // only for recurring
}