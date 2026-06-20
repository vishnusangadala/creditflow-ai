package com.creditflow.analytics.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The single analytics payload the dashboard renders. */
public record AnalyticsResponse(
        long totalWorkflows,
        Map<String, Long> workflowsByStatus,
        Map<String, Long> reviewsByStatus,
        double reviewApprovalRate,
        Map<String, Double> avgLatencyMsByAgent,
        Map<String, Long> verificationByStatus,
        Map<String, Long> failureCategoryDistribution,
        long totalTokens,
        List<EvalTrendPoint> evalTrend
) {
    public record EvalTrendPoint(UUID runId, UUID datasetId, Double overallScore, Instant finishedAt) {}
}
