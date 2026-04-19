package com.starto.controller;

import com.starto.dto.OfferRequestDTO;
import com.starto.model.Offer;
import com.starto.model.User;
import com.starto.enums.Plan;
import com.starto.service.OfferService;
import com.starto.service.PlanService;
import com.starto.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final UserService userService;
    private final PlanService planService;

    // 🔹 helper
    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return userService
                .getUserByFirebaseUid(authentication.getPrincipal().toString())
                .orElse(null);
    }

    // talent sends offer
    @PostMapping("/request")
    public ResponseEntity<?> sendOffer(
            Authentication authentication,
            @RequestBody OfferRequestDTO dto) {

        User user = getUser(authentication);
        if (user == null)
            return ResponseEntity.status(401).build();

        // 🔥 Use safe parser (VERY IMPORTANT)
        Plan plan = user.getPlan();

        // 🔥 Get current usage
        int usedOffers = offerService.countUserOffers(user.getId());

        // 🔥 Enforce limit
        if (!planService.canSendOffer(plan, usedOffers)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Offer limit reached",
                    "upgradeUrl", "/api/subscriptions/upgrade"));
        }

        return ResponseEntity.ok(offerService.sendOffer(user, dto));
    }

    // founder sees inbox
    @GetMapping("/inbox")
    public ResponseEntity<?> getInbox(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(offerService.getAllOffers(user.getId()));
    }

    // talent sees sent
    @GetMapping("/sent")
    public ResponseEntity<?> getSent(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(offerService.getSentOffers(user.getId()));
    }

    // 🔥 WhatsApp link with PLAN CONTROL
    @GetMapping("/{offerId}/whatsapp")
    public ResponseEntity<?> getWhatsappLink(
            Authentication authentication,
            @PathVariable UUID offerId) {

        User user = getUser(authentication);
        if (user == null)
            return ResponseEntity.status(401).build();

        Offer offer = offerService.getOfferById(offerId);

        // 🔥 Convert plan
        Plan plan = Plan.valueOf(user.getPlan().name().toUpperCase());

        // ✅ Founder always allowed
        if (offer.getReceiverId().equals(user.getId())) {
            String link = offerService.getWhatsappLink(user, offerId);
            return ResponseEntity.ok(Map.of("whatsappUrl", link));
        }

        // ✅ Talent (plan-based access)
        if (offer.getRequesterId().equals(user.getId())) {

            if (planService.isWhatsappUnlocked(plan)) {
                String link = offerService.getWhatsappLink(user, offerId);
                return ResponseEntity.ok(Map.of("whatsappUrl", link));
            } else {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Upgrade your plan to unlock WhatsApp",
                        "upgradeUrl", "/api/subscriptions/upgrade"));
            }
        }

        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }
}