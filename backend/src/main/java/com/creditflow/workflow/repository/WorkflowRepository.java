package com.creditflow.workflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findAllByOrderByCreatedAtDesc();
}
