package com.creditflow.review.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.review.domain.Correction;

public interface CorrectionRepository extends JpaRepository<Correction, UUID> {
    List<Correction> findByWorkflowIdOrderByCreatedAt(UUID workflowId);
}
