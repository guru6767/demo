package com.starto.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.starto.model.User;
import com.starto.repository.UserRepository;
import com.starto.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.plan.id}")
    private String planId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // Step 1: Create Razorpay subscription and return sub ID to frontend
    public String createSubscription(String firebaseUid) throws RazorpayException {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        JSONObject subRequest = new JSONObject();
        subRequest.put("plan_id", planId);
        subRequest.put("total_count", 12); // 12 months
        subRequest.put("quantity", 1);
        subRequest.put("notes", new JSONObject().put("firebase_uid", firebaseUid));

        Subscription subscription = razorpayClient.subscriptions.create(subRequest);
        String razorpaySubId = subscription.get("id");

        // Save pending subscription in DB
        com.starto.model.Subscription sub = com.starto.model.Subscription.builder()
                .user(user)
                .plan("premium")
                .razorpaySubId(razorpaySubId)
                .status("pending")
                .amount(99900)
                .currency("INR")
                .startedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusMonths(1))
                .build();

        subscriptionRepository.save(sub);

        return razorpaySubId;
    }

    // Step 2: Verify payment signature from frontend after payment
    @Transactional
    public void verifyAndActivate(String razorpaySubId, String razorpayPaymentId, String razorpaySignature) {
        // Verify signature
        String payload = razorpayPaymentId + "|" + razorpaySubId;
        if (!verifySignature(payload, razorpaySignature)) {
            throw new RuntimeException("Invalid payment signature");
        }

        // Find subscription in DB
        subscriptionRepository.findAll().stream()
                .filter(s -> razorpaySubId.equals(s.getRazorpaySubId()))
                .findFirst()
                .ifPresent(sub -> {
                    sub.setStatus("active");
                    sub.setPaymentId(razorpayPaymentId);
                    subscriptionRepository.save(sub);

                    // Upgrade user plan
                    User user = sub.getUser();
                    user.setPlan("premium");
                    user.setPlanExpiresAt(OffsetDateTime.now().plusMonths(1));
                    userRepository.save(user);

                    log.info("User {} upgraded to premium", user.getFirebaseUid());
                });
    }

    // Step 3: Webhook handler — auto renew every month
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            throw new RuntimeException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        if (eventType.equals("subscription.charged")) {
            JSONObject subData = event
                    .getJSONObject("payload")
                    .getJSONObject("subscription")
                    .getJSONObject("entity");

            String razorpaySubId = subData.getString("id");
            String firebaseUid = subData.getJSONObject("notes").getString("firebase_uid");

            upgradeUserPlan(firebaseUid, "premium", 1);
            log.info("Subscription renewed for {}", firebaseUid);
        }

        if (eventType.equals("subscription.cancelled") || eventType.equals("subscription.expired")) {
            JSONObject subData = event
                    .getJSONObject("payload")
                    .getJSONObject("subscription")
                    .getJSONObject("entity");

            String firebaseUid = subData.getJSONObject("notes").getString("firebase_uid");
            upgradeUserPlan(firebaseUid, "free", 0);
            log.info("Subscription cancelled for {}", firebaseUid);
        }
    }

    @Transactional
    public void upgradeUserPlan(String firebaseUid, String plan, int durationMonths) {
        userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
            user.setPlan(plan);
            user.setPlanExpiresAt(durationMonths > 0
                    ? OffsetDateTime.now().plusMonths(durationMonths)
                    : null);
            userRepository.save(user);
        });
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        return verifySignature(payload, signature);
    }

    public boolean canPerformAction(User user, String feature) {
        return user.getPlan() != null && user.getPlan().equals("premium");
    }
}