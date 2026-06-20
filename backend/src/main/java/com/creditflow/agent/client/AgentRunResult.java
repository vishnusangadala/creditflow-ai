package com.creditflow.agent.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The response returned by the Python agent service's {@code /run} endpoint.
 * Mirrors {@code app/domain/models.py::RunResponse}. Snake_case naming is applied
 * locally (the Python side serializes snake_case) without touching the backend's
 * own camelCase API. Free-form agent outputs are kept as {@link JsonNode} so they
 * can be stored verbatim as JSONB.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRunResult(
        String workflowId,
        String status,
        String borrowerName,
        String riskCategory,
        List<ProcessedDocument> documents,
        List<AgentRunTelemetry> agentRuns,
        JsonNode extraction,
        JsonNode metrics,
        JsonNode risk,
        JsonNode memo,
        Verification verification,
        String errorMessage
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessedDocument(String filename, Integer pageCount, Integer charCount) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentRunTelemetry(
            String agentType,
            String status,
            String model,
            Integer latencyMs,
            Map<String, Integer> tokenUsage,
            String langsmithRunId,
            String errorMessage
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Verification(String overall, List<Check> checks) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Check(
            String checkName,
            String category,
            String target,
            String status,
            String expected,
            String actual,
            String evidence
    ) {}
}
