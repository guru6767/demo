package com.starto.controller;

import com.starto.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // Talent calls this to start subscription
    @PostMapping("/create")
    public ResponseEntity<?> createSubscription(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        try {
            String firebaseUid = authentication.getPrincipal().toString();
            String subId = subscriptionService.createSubscription(firebaseUid);
            return ResponseEntity.ok(Map.of("subscriptionId", subId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Frontend calls this after payment success
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        try {
            subscriptionService.verifyAndActivate(
                    body.get("razorpay_subscription_id"),
                    body.get("razorpay_payment_id"),
                    body.get("razorpay_signature"));
            return ResponseEntity.ok(Map.of("message", "Payment verified, plan upgraded to premium"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Razorpay calls this automatically every month
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            subscriptionService.handleWebhook(payload, signature);
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
        return ResponseEntity.ok().build();
    }
}