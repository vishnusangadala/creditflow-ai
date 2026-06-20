package com.creditflow.evaluation.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Per-case score within an eval run, with the per-dimension breakdown. */
@Entity
@Table(name = "eval_case_results")
public class EvalCaseResult {

    @Id
    private UUID id;

    @Column(name = "eval_run_id", nullable = false)
    private UUID evalRunId;

    @Column(name = "eval_case_id", nullable = false)
    private UUID evalCaseId;

    @Column
    private Double score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimension_scores", columnDefinition = "jsonb")
    private String dimensionScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvalCaseResult() {
        // for JPA
    }

    public static EvalCaseResult create(UUID evalRunId, UUID evalCaseId, Double score,
                                        String dimensionScoresJson, String detailsJson) {
        EvalCaseResult r = new EvalCaseResult();
        r.id = UUID.randomUUID();
        r.evalRunId = evalRunId;
        r.evalCaseId = evalCaseId;
        r.score = score;
        r.dimensionScores = dimensionScoresJson;
        r.details = detailsJson;
        r.createdAt = Instant.now();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getEvalRunId() { return evalRunId; }
    public UUID getEvalCaseId() { return evalCaseId; }
    public Double getScore() { return score; }
    public String getDimensionScores() { return dimensionScores; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
