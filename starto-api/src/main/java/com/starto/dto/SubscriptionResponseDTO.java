// SubscriptionResponseDTO.java
package com.starto.dto;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponseDTO {
    private UUID id;
    private String plan;
    private String status;
    private Integer amountPaid;
    private String razorpayOrderId;
    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private String razorpaySubscriptionId;
}