package com.starto.repository;

import com.starto.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.starto.model.Subscription;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);

    // with this
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    List<com.starto.model.Subscription> findActiveByUserId(@Param("userId") UUID userId);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}