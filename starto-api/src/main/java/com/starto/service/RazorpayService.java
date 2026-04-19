package com.starto.service;

import com.razorpay.Order;
import com.razorpay.Utils;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    private RazorpayClient razorpayClient;

    // ✅ ADD THESE
    private final String keyId;
    private final String keySecret;

    // 🔥 Constructor injection
    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) throws Exception {

        this.keyId = keyId;
        this.keySecret = keySecret;
        this.razorpayClient = new RazorpayClient(keyId, keySecret);

        System.out.println("==== RAZORPAY DEBUG ====");
        System.out.println("KEY ID: " + keyId);
        System.out.println("KEY SECRET: " + keySecret);
        System.out.println("========================");
    }

    // ✅ CREATE ORDER
    public String createOrder(int amountPaise) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("receipt", "starto_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);
            return order.get("id");

        } catch (Exception e) {
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    // ✅ VERIFY PAYMENT (NO NEED NEW CLIENT)
    public boolean verifyPayment(String orderId, String paymentId) {
        try {
            var payment = razorpayClient.payments.fetch(paymentId);
            return orderId.equals(payment.get("order_id"));
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ VERIFY SIGNATURE
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            return Utils.verifySignature(payload, signature, keySecret);
        } catch (Exception e) {
            return false;
        }
    }

    public String createSubscription(String razorpayPlanId) {
        try {
            JSONObject options = new JSONObject();
            options.put("plan_id", razorpayPlanId);
            options.put("customer_notify", 1);
            options.put("total_count", 12); // billing cycles

            var subscription = razorpayClient.subscriptions.create(options);

            return subscription.get("id");

        } catch (Exception e) {
            throw new RuntimeException("Subscription creation failed");
        }
    }

    public JSONObject fetchPayment(String paymentId) {
        try {
            Object payment = razorpayClient.payments.fetch(paymentId);
            return new JSONObject(payment.toString());
        } catch (Exception e) {
            throw new RuntimeException("Payment fetch failed: " + e.getMessage());
        }
    }

}