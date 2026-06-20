package com.creditflow.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.evaluation.domain.EvalCaseResult;

public interface EvalCaseResultRepository extends JpaRepository<EvalCaseResult, UUID> {
    List<EvalCaseResult> findByEvalRunId(UUID evalRunId);
}
