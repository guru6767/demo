package com.starto.repository;

import com.starto.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    // check if talent already sent offer to this signal
    @Query("SELECT o FROM Offer o WHERE o.requester.id = :requesterId AND o.signal.id = :signalId")
    Optional<Offer> findByRequesterIdAndSignalId(
            @Param("requesterId") UUID requesterId,
            @Param("signalId") UUID signalId);

    // founder sees pending offers
    @Query("SELECT o FROM Offer o WHERE o.receiver.id = :receiverId AND o.status = 'pending'")
    List<Offer> findPendingByReceiverId(@Param("receiverId") UUID receiverId);

    // talent sees sent offers
    @Query("SELECT o FROM Offer o WHERE o.requester.id = :requesterId")
    List<Offer> findByRequesterId(@Param("requesterId") UUID requesterId);

    // get accepted offers
    @Query("SELECT o FROM Offer o WHERE o.receiver.id = :receiverId AND o.status = 'accepted'")
    List<Offer> findAcceptedByReceiverId(@Param("receiverId") UUID receiverId);

    @Query("SELECT o FROM Offer o WHERE o.receiver.id = :receiverId ORDER BY o.createdAt DESC")
    List<Offer> findAllByReceiverId(@Param("receiverId") UUID receiverId);

    int countByRequesterId(UUID requesterId);
}