package com.creditflow.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.evaluation.domain.EvalRun;

public interface EvalRunRepository extends JpaRepository<EvalRun, UUID> {
    List<EvalRun> findByDatasetIdOrderByStartedAtDesc(UUID datasetId);
    List<EvalRun> findTop20ByOrderByStartedAtDesc();
}
