package com.creditflow.analytics.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditflow.agent.domain.AgentRun;
import com.creditflow.agent.domain.VerificationResult;
import com.creditflow.agent.repository.AgentRunRepository;
import com.creditflow.agent.repository.VerificationResultRepository;
import com.creditflow.analytics.api.AnalyticsResponse;
import com.creditflow.evaluation.domain.EvalRun;
import com.creditflow.evaluation.domain.EvalRunStatus;
import com.creditflow.evaluation.repository.EvalRunRepository;
import com.creditflow.review.domain.Correction;
import com.creditflow.review.domain.Review;
import com.creditflow.review.domain.ReviewStatus;
import com.creditflow.review.repository.CorrectionRepository;
import com.creditflow.review.repository.ReviewRepository;
import com.creditflow.workflow.domain.Workflow;
import com.creditflow.workflow.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Read-side aggregation over the whole system for the analytics dashboard.
 *
 * <p>Phase 2 computes these in memory over {@code findAll()} for simplicity and
 * because the demo dataset is small. The honest production move is SQL
 * {@code GROUP BY} aggregates or a reporting table — noted, not built.
 */
@Service
public class AnalyticsService {

    private final WorkflowRepository workflowRepository;
    private final ReviewRepository reviewRepository;
    private final AgentRunRepository agentRunRepository;
    private final VerificationResultRepository verificationRepository;
    private final CorrectionRepository correctionRepository;
    private final EvalRunRepository evalRunRepository;
    private final ObjectMapper mapper;

    public AnalyticsService(WorkflowRepository workflowRepository, ReviewRepository reviewRepository,
                            AgentRunRepository agentRunRepository,
                            VerificationResultRepository verificationRepository,
                            CorrectionRepository correctionRepository,
                            EvalRunRepository evalRunRepository, ObjectMapper mapper) {
        this.workflowRepository = workflowRepository;
        this.reviewRepository = reviewRepository;
        this.agentRunRepository = agentRunRepository;
        this.verificationRepository = verificationRepository;
        this.correctionRepository = correctionRepository;
        this.evalRunRepository = evalRunRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse compute() {
        List<Workflow> workflows = workflowRepository.findAll();
        Map<String, Long> workflowsByStatus = new TreeMap<>(workflows.stream()
                .collect(Collectors.groupingBy(w -> w.getStatus().name(), Collectors.counting())));

        List<Review> reviews = reviewRepository.findAll();
        Map<String, Long> reviewsByStatus = new TreeMap<>(reviews.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting())));
        long decided = reviews.stream().filter(Review::isDecided).count();
        long approved = reviewRepository.countByStatus(ReviewStatus.APPROVED);
        double approvalRate = decided == 0 ? 0.0 : round((double) approved / decided);

        List<AgentRun> runs = agentRunRepository.findAll();
        Map<String, Double> avgLatency = new TreeMap<>(runs.stream()
                .filter(r -> r.getLatencyMs() != null)
                .collect(Collectors.groupingBy(r -> r.getAgentType().name(),
                        Collectors.averagingInt(AgentRun::getLatencyMs))));
        long totalTokens = runs.stream().mapToLong(this::totalTokens).sum();

        List<VerificationResult> checks = verificationRepository.findAll();
        Map<String, Long> verificationByStatus = new TreeMap<>(checks.stream()
                .collect(Collectors.groupingBy(v -> v.getStatus().name(), Collectors.counting())));

        List<Correction> corrections = correctionRepository.findAll();
        Map<String, Long> failureDist = new TreeMap<>(corrections.stream()
                .collect(Collectors.groupingBy(c -> c.getFailureCategory().name(), Collectors.counting())));

        List<AnalyticsResponse.EvalTrendPoint> evalTrend = evalRunRepository.findTop20ByOrderByStartedAtDesc()
                .stream()
                .filter(r -> r.getStatus() == EvalRunStatus.COMPLETED)
                .map(this::toTrendPoint)
                .toList();

        return new AnalyticsResponse(
                workflows.size(), workflowsByStatus, reviewsByStatus, approvalRate,
                avgLatency, verificationByStatus, failureDist, totalTokens, evalTrend);
    }

    private long totalTokens(AgentRun run) {
        if (run.getTokenUsage() == null) {
            return 0;
        }
        try {
            var node = mapper.readTree(run.getTokenUsage());
            return node.hasNonNull("total_tokens") ? node.get("total_tokens").asLong() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private AnalyticsResponse.EvalTrendPoint toTrendPoint(EvalRun r) {
        return new AnalyticsResponse.EvalTrendPoint(r.getId(), r.getDatasetId(),
                r.getOverallScore(), r.getFinishedAt());
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
