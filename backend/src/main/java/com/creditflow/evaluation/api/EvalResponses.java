package com.creditflow.evaluation.api;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public final class EvalResponses {

    private EvalResponses() {}

    public record DatasetView(
            UUID id,
            String name,
            String description,
            long caseCount,
            Instant createdAt
    ) {}

    public record CaseView(
            UUID id,
            UUID datasetId,
            String name,
            UUID sourceWorkflowId,
            JsonNode expected,
            Instant createdAt
    ) {}

    public record RunView(
            UUID id,
            UUID datasetId,
            String status,
            Double overallScore,
            JsonNode scores,
            Integer caseCount,
            Instant startedAt,
            Instant finishedAt
    ) {}

    public record CaseResultView(
            UUID id,
            UUID evalCaseId,
            Double score,
            JsonNode dimensionScores,
            Instant createdAt
    ) {}
}
