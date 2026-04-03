package com.starto.service;

import com.starto.dto.OfferRequestDTO;
import com.starto.model.Offer;
import com.starto.model.Signal;
import com.starto.model.User;
import com.starto.repository.OfferRepository;
import com.starto.repository.SignalRepository;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;

    // talent sends offer
    @Transactional
    public Offer sendOffer(User talent, OfferRequestDTO dto) {

        // block founders from sending offers
        if (talent.getRole().equalsIgnoreCase("Founder")) {
            throw new RuntimeException("Founders cannot send offers");
        }

        Signal signal = signalRepository.findById(dto.getSignalId())
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        // one offer per signal
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
                .status("pending")
                .build();

        // increment offerCount when talent sends
        signal.setOfferCount(signal.getOfferCount() + 1);
        signalRepository.save(signal);

        return offerRepository.save(offer);
    }

    // founder accepts offer
    @Transactional
    public Offer acceptOffer(User founder, UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        if (!offer.getReceiverId().equals(founder.getId())) {
            throw new RuntimeException("Forbidden: not your offer");
        }

        offer.setStatus("accepted");
        return offerRepository.save(offer);
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

        if (!offer.getStatus().equals("accepted")) {
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
}