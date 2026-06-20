package com.creditflow.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.evaluation.domain.EvalCase;

public interface EvalCaseRepository extends JpaRepository<EvalCase, UUID> {
    List<EvalCase> findByDatasetIdOrderByCreatedAt(UUID datasetId);
    long countByDatasetId(UUID datasetId);
}
