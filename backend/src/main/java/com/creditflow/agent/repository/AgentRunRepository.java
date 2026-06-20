package com.creditflow.agent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.agent.domain.AgentRun;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
    List<AgentRun> findByWorkflowIdOrderByStartedAt(UUID workflowId);
}
