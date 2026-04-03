package com.starto.service;

import com.starto.model.Review;
import com.starto.model.User;
import com.starto.repository.ReviewRepository;
import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review addReview(User reviewer, UUID reviewedUserId, int rating, String comment) {

        // can't review yourself
        if (reviewer.getId().equals(reviewedUserId)) {
            throw new RuntimeException("You cannot review yourself");
        }

        // one review per user
        reviewRepository.findByReviewerIdAndReviewedId(reviewer.getId(), reviewedUserId)
                .ifPresent(r -> {
                    throw new RuntimeException("You already reviewed this user");
                });

        // rating must be 1-5
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        User reviewed = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    public List<Review> getReviews(UUID reviewedUserId) {
        return reviewRepository.findByReviewedId(reviewedUserId);
    }

    public Map<String, Object> getReviewSummary(UUID reviewedUserId) {
        Double avg = reviewRepository.findAverageRatingByReviewedId(reviewedUserId);
        long total = reviewRepository.countByReviewedId(reviewedUserId);

        return Map.of(
                "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                "totalReviews", total);
    }
}