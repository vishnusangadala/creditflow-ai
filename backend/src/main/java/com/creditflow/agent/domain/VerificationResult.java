package com.creditflow.agent.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One verification check produced by the Verifier agent (grounding,
 * recomputation, or memo alignment). The collection of these per workflow is the
 * evidence that the AI output can — or cannot — be trusted.
 */
@Entity
@Table(name = "verification_results")
public class VerificationResult {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "agent_run_id")
    private UUID agentRunId;

    @Column(name = "check_name", nullable = false, length = 255)
    private String checkName;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(length = 512)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CheckStatus status;

    @Column
    private String expected;

    @Column
    private String actual;

    @Column
    private String evidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VerificationResult() {
        // for JPA
    }

    public static VerificationResult create(UUID workflowId, UUID agentRunId, String checkName,
                                            String category, String target, CheckStatus status,
                                            String expected, String actual, String evidence) {
        VerificationResult v = new VerificationResult();
        v.id = UUID.randomUUID();
        v.workflowId = workflowId;
        v.agentRunId = agentRunId;
        v.checkName = checkName;
        v.category = category;
        v.target = target;
        v.status = status;
        v.expected = expected;
        v.actual = actual;
        v.evidence = evidence;
        v.createdAt = Instant.now();
        return v;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getAgentRunId() { return agentRunId; }
    public String getCheckName() { return checkName; }
    public String getCategory() { return category; }
    public String getTarget() { return target; }
    public CheckStatus getStatus() { return status; }
    public String getExpected() { return expected; }
    public String getActual() { return actual; }
    public String getEvidence() { return evidence; }
    public Instant getCreatedAt() { return createdAt; }
}
