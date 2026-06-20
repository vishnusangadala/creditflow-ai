package com.creditflow.agent.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Telemetry for a single agent invocation: which agent, success/failure,
 * latency, model, token usage, and the LangSmith trace id. This is the
 * audit/observability record — distinct from the artifact it produced
 * ({@link AgentOutput}).
 */
@Entity
@Table(name = "agent_runs")
public class AgentRun {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 32)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RunStatus status;

    @Column(length = 128)
    private String model;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_usage", columnDefinition = "jsonb")
    private String tokenUsage;

    @Column(name = "langsmith_run_id", length = 255)
    private String langsmithRunId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AgentRun() {
        // for JPA
    }

    public static AgentRun create(UUID workflowId, AgentType agentType, RunStatus status,
                                  String model, Integer latencyMs, String tokenUsageJson,
                                  String langsmithRunId, String errorMessage) {
        AgentRun r = new AgentRun();
        r.id = UUID.randomUUID();
        r.workflowId = workflowId;
        r.agentType = agentType;
        r.status = status;
        r.model = model;
        r.latencyMs = latencyMs;
        r.tokenUsage = tokenUsageJson;
        r.langsmithRunId = langsmithRunId;
        r.errorMessage = errorMessage;
        Instant now = Instant.now();
        r.startedAt = now;
        r.finishedAt = now;
        return r;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public AgentType getAgentType() { return agentType; }
    public RunStatus getStatus() { return status; }
    public String getModel() { return model; }
    public Integer getLatencyMs() { return latencyMs; }
    public String getTokenUsage() { return tokenUsage; }
    public String getLangsmithRunId() { return langsmithRunId; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
