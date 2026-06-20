package com.creditflow.agent.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The structured artifact an agent produced (extracted fields, metrics, memo
 * text, etc.), stored as JSONB and linked 1:1 to its {@link AgentRun}. Keeping
 * the artifact separate from the run telemetry means a re-run never overwrites
 * history.
 */
@Entity
@Table(name = "agent_outputs")
public class AgentOutput {

    @Id
    private UUID id;

    @Column(name = "agent_run_id", nullable = false)
    private UUID agentRunId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String output;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentOutput() {
        // for JPA
    }

    public static AgentOutput create(UUID agentRunId, String outputJson) {
        AgentOutput o = new AgentOutput();
        o.id = UUID.randomUUID();
        o.agentRunId = agentRunId;
        o.output = outputJson;
        o.createdAt = Instant.now();
        return o;
    }

    public UUID getId() { return id; }
    public UUID getAgentRunId() { return agentRunId; }
    public String getOutput() { return output; }
    public Instant getCreatedAt() { return createdAt; }
}
