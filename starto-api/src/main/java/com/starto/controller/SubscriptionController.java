package com.starto.controller;

import com.starto.dto.SubscriptionRequestDTO;
import com.starto.dto.SubscriptionResponseDTO;
import com.starto.model.PlanEntity;
import com.starto.service.PlanServiceDB;
import com.starto.service.SubscriptionService;
import com.starto.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Subscription and payment controller.
 *
 * <p>Manages the full Razorpay payment lifecycle: plan listing → order creation →
 * webhook verification → plan activation. Also exposes subscription status and
 * payment history for authenticated users.</p>
 */
@Tag(name = "Subscriptions", description = "Plan listing, Razorpay order creation, payment verification, and subscription status")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final PlanServiceDB planServiceDB;

    /**
     * Create a Razorpay order for the requested plan.
     *
     * <p>The client receives a Razorpay {@code orderId} and uses it to open
     * the Razorpay checkout widget. After payment, call {@link #verifyPayment}.</p>
     *
     * @param dto contains the target plan name
     * @return Razorpay order details ({@code orderId}, {@code amount}, {@code currency})
     */
    @Operation(summary = "Create Razorpay order",
               description = "Generates a Razorpay order for the given plan. Pass the returned orderId to Razorpay SDK.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Razorpay order created"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @RequestBody SubscriptionRequestDTO dto) {

        if (authentication == null) return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        subscriptionService.createOrder(user, dto.getPlan())
                ))
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Verify a completed Razorpay payment and activate the subscription.
     *
     * <p>Accepts both order-flow ({@code razorpayOrderId}) and subscription-flow
     * ({@code razorpaySubscriptionId}) payloads. Validates the HMAC signature
     * and upgrades the user's plan on success.</p>
     *
     * @param body map containing Razorpay payment identifiers and signature
     * @return success confirmation string or error details
     */
    @Operation(summary = "Verify Razorpay payment",
               description = "Validates HMAC signature and activates plan. Call after successful Razorpay SDK callback.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment verified and plan activated"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid payment fields")
    })
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {

    String orderId = body.get("razorpayOrderId");
    String subscriptionId = body.get("razorpaySubscriptionId");
    String paymentId = body.get("razorpayPaymentId");
    String signature = body.get("razorpaySignature");

    //  ORDER FLOW
    if (orderId != null) {
        if (paymentId == null || signature == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing order fields"));
        }

        subscriptionService.activateSubscription(orderId, paymentId, signature);
        return ResponseEntity.ok("Order verified");
    }

    //  SUBSCRIPTION FLOW
    if (subscriptionId != null) {
        if (paymentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing subscription fields"));
        }

        subscriptionService.activateSubscriptionBySubscription(subscriptionId, paymentId);
        return ResponseEntity.ok("Subscription verified");
    }

    return ResponseEntity.badRequest()
            .body(Map.of("error", "Invalid request type"));
}
@GetMapping("/history")
public ResponseEntity<?> getPaymentHistory(Authentication authentication) {
    if (authentication == null) return ResponseEntity.status(401).build();

    return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
            .map(user -> ResponseEntity.ok(
                    subscriptionService.getPaymentHistory(user.getId())
            ))
            .orElse(ResponseEntity.status(401).build());
}

@PostMapping("/upgrade")
public ResponseEntity<?> upgradePlan(
        Authentication authentication,
        @RequestBody SubscriptionRequestDTO dto) {

    if (authentication == null) return ResponseEntity.status(401).build();

    return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
            .map(user -> {
                try {
                    SubscriptionResponseDTO response =
                        subscriptionService.upgradePlan(user, dto.getPlan());

                    return ResponseEntity.ok(Map.of(
                        "orderId", response.getRazorpayOrderId(),
                        "amount", response.getAmountPaid(),
                        "plan", response.getPlan(),
                        "currency", "INR",
                        "message", "Complete payment to upgrade to " + dto.getPlan() + " plan"
                    ));
                } catch (RuntimeException ex) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", ex.getMessage()));
                }
            })
            .orElse(ResponseEntity.status(401).build());
}

    /**
     * List all available subscription plans and their prices.
     *
     * <p>Public endpoint — no authentication required. Used by the pricing page
     * to render plan cards with live data from the DB.</p>
     *
     * @return list of plan objects with {@code plan}, {@code amountRupees}, {@code durationDays}
     */
    @Operation(summary = "List subscription plans",
               description = "Returns all plans with pricing. Public endpoint — no auth required.")
    @ApiResponse(responseCode = "200", description = "Plan list")
    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {

    List<PlanEntity> plans = planServiceDB.getAllPlans();

    List<Map<String, Object>> response = plans.stream()
            .map(p -> Map.<String, Object>of(
                    "plan", p.getCode().name(),
                    "amountPaise", p.getPricePaise(),
                    "amountRupees", p.getPricePaise() / 100.0,
                    "durationDays", p.getDurationDays(),
                    "billingType", p.getBillingType().name()
            ))
            .collect(Collectors.toList());

    return ResponseEntity.ok(response);
}


@GetMapping("/status")
public ResponseEntity<?> getCurrentPlan(Authentication authentication) {

    if (authentication == null) return ResponseEntity.status(401).build();

    return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
            .map(user -> ResponseEntity.ok(
                    subscriptionService.getCurrentPlanStatus(user)
            ))
            .orElse(ResponseEntity.status(401).build());
}

}