package com.starto.service;

import com.starto.dto.OfferRequestDTO;
import com.starto.model.Offer;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.repository.OfferRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.UserRepository;
import com.starto.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;

    // talent sends offer
  @Transactional
public Offer sendOffer(User talent, OfferRequestDTO dto) {


    Signal signal = signalRepository.findById(dto.getSignalId())
            .orElseThrow(() -> new RuntimeException("Signal not found"));

    //  one offer per signal
    offerRepository.findByRequesterIdAndSignalId(talent.getId(), signal.getId())
            .ifPresent(o -> {
                throw new RuntimeException("You already sent an offer for this signal");
            });

    Offer offer = Offer.builder()
            .requester(talent)
            .receiver(signal.getUser())
            .signal(signal)
            .organizationName(dto.getOrganizationName())
            .portfolioLink(dto.getPortfolioLink())
            .message(dto.getMessage())
            .status("PENDING")
            .build();

    // increment safely
    Integer count = signal.getOfferCount();
    signal.setOfferCount((count == null ? 0 : count) + 1);
    signalRepository.save(signal);

    Offer savedOffer = offerRepository.save(offer);

    //  WEBSOCKET (ADD HERE)
    webSocketService.send(
            "/topic/offers/" + savedOffer.getReceiver().getId(),
            Map.of(
                    "type", "NEW_OFFER",
                    "data", savedOffer
            )
    );

    Map<String, Object> data = Map.of(
    "offerId", savedOffer.getId(),
    "signalId", signal.getId(),
    "organizationName", savedOffer.getOrganizationName(),
    "action", "NEW_OFFER"
);

   notificationService.send(
    savedOffer.getReceiver().getId(),
    "NEW_OFFER",
    "New Offer Received!",
    talent.getName() + " sent you an offer",
    data   
);

    return savedOffer;
}

    // founder accepts offer
   @Transactional
public Offer acceptOffer(User founder, UUID offerId) {

    Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));

    //  only receiver can accept
    if (!offer.getReceiver().getId().equals(founder.getId())) {
        throw new RuntimeException("Not authorized to accept this offer");
    }

    //  must be pending
    if (!"PENDING".equalsIgnoreCase(offer.getStatus())) {
        throw new RuntimeException("Offer is not in pending state");
    }

    // update status
    offer.setStatus("ACCEPTED");
    offer.setUpdatedAt(OffsetDateTime.now());

    Offer updated = offerRepository.save(offer);

    //  notify talent
    webSocketService.send(
            "/topic/offers/" + updated.getRequester().getId(),
            Map.of(
                    "type", "OFFER_ACCEPTED",
                    "data", updated
            )
    );

    notificationService.send(
    updated.getRequester().getId(),
    "OFFER_ACCEPTED",
    "Offer Accepted!",
    "Your offer was accepted",
    null
);

    return updated;
}

    

    // founder sees pending offers
    public List<Offer> getAllOffers(UUID founderId) {
    return offerRepository.findAllByReceiverId(founderId);
}

    // talent sees sent offers
    public List<Offer> getSentOffers(UUID talentId) {
        return offerRepository.findByRequesterId(talentId);
    }

    // get whatsapp link after acceptance
    public String getWhatsappLink(User user, UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        if (!"ACCEPTED".equalsIgnoreCase(offer.getStatus())) {
            throw new RuntimeException("Offer not accepted yet");
        }

        // verify user is part of this offer
        if (!offer.getRequesterId().equals(user.getId()) &&
            !offer.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("Forbidden");
        }

        // get other person's phone
        UUID otherUserId = offer.getRequesterId().equals(user.getId())
                ? offer.getReceiverId()
                : offer.getRequesterId();

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (otherUser.getPhone() == null || otherUser.getPhone().isBlank()) {
            throw new RuntimeException("User has no phone number registered");
        }

        String phone = otherUser.getPhone().replaceAll("[^0-9]", "");
        return "https://wa.me/" + phone;
    }

    public Offer getOfferById(UUID offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
    }

    public int countUserOffers(UUID userId) {
    return offerRepository.countByRequesterId(userId);
}
}