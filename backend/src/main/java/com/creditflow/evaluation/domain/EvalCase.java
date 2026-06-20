package com.creditflow.evaluation.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One golden case. {@code expected} is the human-verified answer (JSON), often
 * promoted from a corrected workflow — that promotion is the learning loop.
 */
@Entity
@Table(name = "eval_cases")
public class EvalCase {

    @Id
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_workflow_id")
    private UUID sourceWorkflowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String expected;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvalCase() {
        // for JPA
    }

    public static EvalCase create(UUID datasetId, String name, UUID sourceWorkflowId, String expectedJson) {
        EvalCase c = new EvalCase();
        c.id = UUID.randomUUID();
        c.datasetId = datasetId;
        c.name = name;
        c.sourceWorkflowId = sourceWorkflowId;
        c.expected = expectedJson;
        c.createdAt = Instant.now();
        return c;
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public String getName() { return name; }
    public UUID getSourceWorkflowId() { return sourceWorkflowId; }
    public String getExpected() { return expected; }
    public Instant getCreatedAt() { return createdAt; }
}
