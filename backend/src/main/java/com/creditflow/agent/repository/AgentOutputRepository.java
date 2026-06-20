package com.creditflow.agent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.agent.domain.AgentOutput;

public interface AgentOutputRepository extends JpaRepository<AgentOutput, UUID> {
    List<AgentOutput> findByAgentRunIdIn(List<UUID> agentRunIds);
}
