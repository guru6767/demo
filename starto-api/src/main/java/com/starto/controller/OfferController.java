package com.starto.controller;

import com.starto.dto.OfferRequestDTO;
import com.starto.model.Offer;
import com.starto.service.OfferService;
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

    // talent sends offer
    @PostMapping("/request")
    public ResponseEntity<?> sendOffer(
            Authentication authentication,
            @RequestBody OfferRequestDTO dto) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(offerService.sendOffer(user, dto)))
                .orElse(ResponseEntity.status(401).build());
    }

    // founder sees pending offers
    @GetMapping("/inbox")
    public ResponseEntity<List<Offer>> getInbox(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(offerService.getAllOffers(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // talent sees sent offers
    @GetMapping("/sent")
    public ResponseEntity<List<Offer>> getSent(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> ResponseEntity.ok(offerService.getSentOffers(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    // get whatsapp link
    @GetMapping("/{offerId}/whatsapp")
    public ResponseEntity<?> getWhatsappLink(
            Authentication authentication,
            @PathVariable UUID offerId) {

        if (authentication == null)
            return ResponseEntity.status(401).build();

        return userService.getUserByFirebaseUid(authentication.getPrincipal().toString())
                .map(user -> {
                    Offer offer = offerService.getOfferById(offerId);

                    // founder always gets link
                    if (offer.getReceiverId().equals(user.getId())) {
                        String link = offerService.getWhatsappLink(user, offerId);
                        return ResponseEntity.ok(Map.of("whatsappUrl", link));
                    }

                    // talent only if premium
                    if (offer.getRequesterId().equals(user.getId())) {
                        if (user.getPlan() != null && user.getPlan().equalsIgnoreCase("premium")) {
                            String link = offerService.getWhatsappLink(user, offerId);
                            return ResponseEntity.ok(Map.of("whatsappUrl", link));
                        } else {
                            return ResponseEntity.status(403).body(Map.of(
                                    "error", "Upgrade to premium to initiate contact",
                                    "upgradeUrl", "/api/subscriptions/upgrade"));
                        }
                    }

                    return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}