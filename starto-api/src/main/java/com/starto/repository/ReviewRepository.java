package com.starto.repository;

import com.starto.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByReviewedId(UUID reviewedId);

    Optional<Review> findByReviewerIdAndReviewedId(UUID reviewerId, UUID reviewedId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewed.id = :reviewedId")
    Double findAverageRatingByReviewedId(@Param("reviewedId") UUID reviewedId);

    long countByReviewedId(UUID reviewedId);
}