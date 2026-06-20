package com.creditflow.workflow.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * API response shapes for the workflow endpoints, grouped in one file because
 * they form a single cohesive contract. Top-level fields are camelCase; the
 * nested AI artifacts ({@code extraction}, {@code metrics}, ...) are passed
 * through verbatim and keep the agent's own snake_case schema.
 */
public final class WorkflowResponses {

    private WorkflowResponses() {}

    /** Returned from POST /workflows — just enough to start polling. */
    public record Created(UUID id, String status) {}

    /** A row in the workflow list. */
    public record Summary(
            UUID id,
            String status,
            String borrowerName,
            String riskCategory,
            Instant createdAt,
            Instant completedAt
    ) {}

    public record DocumentView(
            UUID id,
            String filename,
            String contentType,
            Long sizeBytes,
            Integer pageCount
    ) {}

    public record AgentRunView(
            UUID id,
            String agentType,
            String status,
            String model,
            Integer latencyMs,
            JsonNode tokenUsage,
            String langsmithRunId,
            String errorMessage,
            Instant startedAt,
            JsonNode output
    ) {}

    public record CheckView(
            String checkName,
            String category,
            String target,
            String status,
            String expected,
            String actual,
            String evidence
    ) {}

    /** Convenience grouping of the structured artifacts for the UI. */
    public record Results(
            JsonNode extraction,
            JsonNode metrics,
            JsonNode risk,
            JsonNode memo
    ) {}

    /** Full workflow detail — the audit-complete view. */
    public record Detail(
            UUID id,
            String status,
            String borrowerName,
            String riskCategory,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            List<DocumentView> documents,
            List<AgentRunView> agentRuns,
            Results results,
            List<CheckView> verification
    ) {}
}
