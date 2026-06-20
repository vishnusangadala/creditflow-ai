package com.creditflow.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.review.domain.Review;
import com.creditflow.review.domain.ReviewStatus;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByWorkflowId(UUID workflowId);

    List<Review> findByStatusInOrderByCreatedAt(List<ReviewStatus> statuses);

    List<Review> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReviewStatus status);
}
