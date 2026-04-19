package com.starto.controller;

import com.starto.dto.SubscriptionRequestDTO;
import com.starto.dto.SubscriptionResponseDTO;
import com.starto.model.PlanEntity;
import com.starto.service.PlanServiceDB;
import com.starto.service.SubscriptionService;
import com.starto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final PlanServiceDB planServiceDB;

    // Frontend calls this to get Razorpay order ID
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @RequestBody SubscriptionRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        subscriptionService.createOrder(user, dto.getPlan())))
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {

        String orderId = body.get("razorpayOrderId");
        String subscriptionId = body.get("razorpaySubscriptionId");
        String paymentId = body.get("razorpayPaymentId");
        String signature = body.get("razorpaySignature");

        // 🔵 ORDER FLOW
        if (orderId != null) {
            if (paymentId == null || signature == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing order fields"));
            }

            subscriptionService.activateSubscription(orderId, paymentId, signature);
            return ResponseEntity.ok("Order verified");
        }

        // 🟢 SUBSCRIPTION FLOW
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
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        subscriptionService.getPaymentHistory(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradePlan(
            Authentication authentication,
            @RequestBody SubscriptionRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> {
                    try {
                        SubscriptionResponseDTO response = subscriptionService.upgradePlan(user, dto.getPlan());

                        return ResponseEntity.ok(Map.of(
                                "orderId", response.getRazorpayOrderId(),
                                "amount", response.getAmountPaid(),
                                "plan", response.getPlan(),
                                "currency", "INR",
                                "message", "Complete payment to upgrade to " + dto.getPlan() + " plan"));
                    } catch (RuntimeException ex) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {

        List<PlanEntity> plans = planServiceDB.getAllPlans();

        List<Map<String, Object>> response = plans.stream()
                .map(p -> Map.<String, Object>of(
                        "plan", p.getCode().name(),
                        "amountPaise", p.getPricePaise(),
                        "amountRupees", p.getPricePaise() / 100.0,
                        "durationDays", p.getDurationDays(),
                        "billingType", p.getBillingType().name()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getCurrentPlan(Authentication authentication) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(
                        subscriptionService.getCurrentPlanStatus(user)))
                .orElse(ResponseEntity.status(401).build());
    }

}