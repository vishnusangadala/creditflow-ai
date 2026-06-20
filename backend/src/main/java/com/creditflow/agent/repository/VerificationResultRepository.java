package com.creditflow.agent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.agent.domain.VerificationResult;

public interface VerificationResultRepository extends JpaRepository<VerificationResult, UUID> {
    List<VerificationResult> findByWorkflowIdOrderByCreatedAt(UUID workflowId);
}
