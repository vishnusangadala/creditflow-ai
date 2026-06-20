package com.creditflow.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.evaluation.domain.EvalDataset;

public interface EvalDatasetRepository extends JpaRepository<EvalDataset, UUID> {
    List<EvalDataset> findAllByOrderByCreatedAtDesc();
}
