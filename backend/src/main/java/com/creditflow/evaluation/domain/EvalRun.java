package com.creditflow.evaluation.domain;

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

/** One execution of an evaluation over a dataset, with aggregated scores. */
@Entity
@Table(name = "eval_runs")
public class EvalRun {

    @Id
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvalRunStatus status;

    @Column(name = "overall_score")
    private Double overallScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String scores;

    @Column(name = "case_count")
    private Integer caseCount;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected EvalRun() {
        // for JPA
    }

    public static EvalRun start(UUID datasetId) {
        EvalRun r = new EvalRun();
        r.id = UUID.randomUUID();
        r.datasetId = datasetId;
        r.status = EvalRunStatus.RUNNING;
        r.startedAt = Instant.now();
        return r;
    }

    public void complete(double overallScore, String scoresJson, int caseCount) {
        this.status = EvalRunStatus.COMPLETED;
        this.overallScore = overallScore;
        this.scores = scoresJson;
        this.caseCount = caseCount;
        this.finishedAt = Instant.now();
    }

    public void fail() {
        this.status = EvalRunStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public EvalRunStatus getStatus() { return status; }
    public Double getOverallScore() { return overallScore; }
    public String getScores() { return scores; }
    public Integer getCaseCount() { return caseCount; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
