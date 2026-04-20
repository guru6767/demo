package com.starto.repository;

import com.starto.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    @Query("SELECT c FROM Connection c WHERE c.requester.id = :requesterId OR c.receiver.id = :receiverId")
    List<Connection> findByRequesterIdOrReceiverId(@Param("requesterId") UUID requesterId, @Param("receiverId") UUID receiverId);

    @Query("SELECT c FROM Connection c WHERE c.receiver.id = :receiverId AND c.status = :status")
    List<Connection> findByReceiverIdAndStatus(@Param("receiverId") UUID receiverId, @Param("status") String status);

    @Query("SELECT c FROM Connection c WHERE c.requester.id = :requesterId AND c.status = :status")
    List<Connection> findByRequesterIdAndStatus(@Param("requesterId") UUID requesterId, @Param("status") String status);

    @Query("SELECT c FROM Connection c WHERE c.requester.id = :requesterId AND c.signal.id = :signalId")
    Optional<Connection> findByRequesterIdAndSignalId(@Param("requesterId") UUID requesterId, @Param("signalId") UUID signalId);

    @Query("""
        SELECT c FROM Connection c
        WHERE c.status = 'accepted'
        AND (
            (c.requester.id = :userA AND c.receiver.id = :userB)
            OR
            (c.requester.id = :userB AND c.receiver.id = :userA)
        )
    """)
    Optional<Connection> findAcceptedConnection(
        @Param("userA") UUID userA,
        @Param("userB") UUID userB
    );

    @Query("SELECT c FROM Connection c WHERE c.requester.id = :requesterId")
    List<Connection> findByRequesterId(@Param("requesterId") UUID requesterId);

    @Query("SELECT c FROM Connection c WHERE c.requester.id = :requesterId AND c.receiver.id = :receiverId")
Optional<Connection> findByRequesterIdAndReceiverId(
    @Param("requesterId") UUID requesterId,
    @Param("receiverId") UUID receiverId
);


@Query("""
    SELECT c FROM Connection c
    WHERE c.status = 'accepted'
    AND (c.requester.id = :userId OR c.receiver.id = :userId)
""")
List<Connection> findAcceptedByUserId(@Param("userId") UUID userId);

boolean existsByRequester_IdAndReceiver_IdAndStatus(
    UUID requesterId,
    UUID receiverId,
    String status
);

}