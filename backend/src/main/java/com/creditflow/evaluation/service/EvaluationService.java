package com.creditflow.evaluation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditflow.agent.domain.CheckStatus;
import com.creditflow.agent.domain.VerificationResult;
import com.creditflow.agent.repository.VerificationResultRepository;
import com.creditflow.audit.domain.AuditEventType;
import com.creditflow.audit.service.AuditEventService;
import com.creditflow.common.ActorRole;
import com.creditflow.common.ResourceNotFoundException;
import com.creditflow.evaluation.domain.EvalCase;
import com.creditflow.evaluation.domain.EvalCaseResult;
import com.creditflow.evaluation.domain.EvalDataset;
import com.creditflow.evaluation.domain.EvalRun;
import com.creditflow.evaluation.repository.EvalCaseRepository;
import com.creditflow.evaluation.repository.EvalCaseResultRepository;
import com.creditflow.evaluation.repository.EvalDatasetRepository;
import com.creditflow.evaluation.repository.EvalRunRepository;
import com.creditflow.review.domain.Correction;
import com.creditflow.review.repository.CorrectionRepository;
import com.creditflow.workflow.api.WorkflowResponses;
import com.creditflow.workflow.service.WorkflowService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds golden datasets from corrected workflows (the learning loop) and runs
 * deterministic evaluations against them.
 *
 * <p>"Actual" = the raw AI output preserved in {@code agent_outputs}. "Expected"
 * = that output with human corrections applied. Scoring the former against the
 * latter measures agent accuracy using human review as ground truth — and lets
 * us measure whether the Verifier caught the errors humans found.
 */
@Service
public class EvaluationService {

    private static final List<String> EXTRACTION_SCALARS =
            List.of("borrower", "interest_rate", "maturity_date", "collateral");
    private static final List<String> FINANCIAL_KEYS =
            List.of("total_debt", "ebitda", "ebit", "current_assets", "current_liabilities", "interest_expense");
    private static final List<String> METRIC_KEYS =
            List.of("debt_to_ebitda", "current_ratio", "interest_coverage");

    private final EvalDatasetRepository datasetRepository;
    private final EvalCaseRepository caseRepository;
    private final EvalRunRepository runRepository;
    private final EvalCaseResultRepository caseResultRepository;
    private final CorrectionRepository correctionRepository;
    private final VerificationResultRepository verificationRepository;
    private final WorkflowService workflowService;
    private final EvaluationScorer scorer;
    private final AuditEventService audit;
    private final ObjectMapper mapper;

    public EvaluationService(EvalDatasetRepository datasetRepository, EvalCaseRepository caseRepository,
                             EvalRunRepository runRepository, EvalCaseResultRepository caseResultRepository,
                             CorrectionRepository correctionRepository,
                             VerificationResultRepository verificationRepository,
                             WorkflowService workflowService, EvaluationScorer scorer,
                             AuditEventService audit, ObjectMapper mapper) {
        this.datasetRepository = datasetRepository;
        this.caseRepository = caseRepository;
        this.runRepository = runRepository;
        this.caseResultRepository = caseResultRepository;
        this.correctionRepository = correctionRepository;
        this.verificationRepository = verificationRepository;
        this.workflowService = workflowService;
        this.scorer = scorer;
        this.audit = audit;
        this.mapper = mapper;
    }

    // ----------------------------------------------------------------- datasets
    @Transactional
    public EvalDataset createDataset(String name, String description) {
        return datasetRepository.save(EvalDataset.create(name, description));
    }

    @Transactional(readOnly = true)
    public List<EvalDataset> listDatasets() {
        return datasetRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<EvalCase> casesOf(UUID datasetId) {
        return caseRepository.findByDatasetIdOrderByCreatedAt(datasetId);
    }

    /** Learning loop: turn a reviewed workflow into a golden case. */
    @Transactional
    public EvalCase promote(UUID datasetId, UUID workflowId, String caseName) {
        datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));
        WorkflowResponses.Detail detail = workflowService.getWorkflowDetail(workflowId);
        List<Correction> corrections = correctionRepository.findByWorkflowIdOrderByCreatedAt(workflowId);

        String name = (caseName == null || caseName.isBlank())
                ? (detail.borrowerName() == null ? "case-" + workflowId : detail.borrowerName())
                : caseName;
        String expectedJson = buildGolden(detail, corrections);
        EvalCase saved = caseRepository.save(EvalCase.create(datasetId, name, workflowId, expectedJson));

        audit.record(workflowId, AuditEventType.CASE_PROMOTED_TO_DATASET, "system", ActorRole.SYSTEM,
                "Promoted to eval dataset (" + datasetId + ")", null);
        return saved;
    }

    // -------------------------------------------------------------------- runs
    @Transactional
    public EvalRun run(UUID datasetId) {
        datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));
        EvalRun run = runRepository.save(EvalRun.start(datasetId));

        List<EvalCase> cases = caseRepository.findByDatasetIdOrderByCreatedAt(datasetId);
        Map<String, List<Double>> perDimension = new HashMap<>();
        List<Double> caseScores = new ArrayList<>();

        for (EvalCase c : cases) {
            if (c.getSourceWorkflowId() == null) {
                continue; // can only score cases tied to a real workflow run
            }
            EvaluationScorer.CaseScore score = scoreCase(c);
            caseScores.add(score.overall());
            for (EvaluationScorer.DimensionScore d : score.dimensions()) {
                perDimension.computeIfAbsent(d.dimension(), k -> new ArrayList<>()).add(d.score());
            }
            caseResultRepository.save(EvalCaseResult.create(
                    run.getId(), c.getId(), score.overall(),
                    toJson(score.dimensions()), null));
        }

        double overall = caseScores.isEmpty() ? 0.0
                : round(caseScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Map<String, Double> aggregated = new HashMap<>();
        perDimension.forEach((dim, vals) ->
                aggregated.put(dim, round(vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))));

        run.complete(overall, toJson(aggregated), caseScores.size());
        runRepository.save(run);

        audit.record(null, AuditEventType.EVAL_RUN_COMPLETED, "system", ActorRole.SYSTEM,
                "Eval run over dataset " + datasetId + " scored " + overall, aggregated);
        return run;
    }

    @Transactional(readOnly = true)
    public List<EvalRun> runsOf(UUID datasetId) {
        return runRepository.findByDatasetIdOrderByStartedAtDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public List<EvalRun> recentRuns() {
        return runRepository.findTop20ByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<EvalCaseResult> resultsOf(UUID runId) {
        return caseResultRepository.findByEvalRunId(runId);
    }

    // ----------------------------------------------------------------- scoring
    private EvaluationScorer.CaseScore scoreCase(EvalCase c) {
        WorkflowResponses.Detail detail = workflowService.getWorkflowDetail(c.getSourceWorkflowId());
        JsonNode expected = readTree(c.getExpected());

        Map<String, String> actualExtraction = extractionMap(detail.results().extraction());
        Map<String, Double> actualMetrics = metricsMap(detail.results().metrics());
        String actualRisk = riskCategory(detail.results().risk());

        Map<String, String> expectedExtraction = stringMap(expected.get("extraction"));
        Map<String, Double> expectedMetrics = doubleMap(expected.get("metrics"));
        String expectedRisk = expected.hasNonNull("risk") ? expected.get("risk").asText() : null;

        Set<String> corrected = new HashSet<>();
        for (Correction corr : correctionRepository.findByWorkflowIdOrderByCreatedAt(c.getSourceWorkflowId())) {
            corrected.add(corr.getFieldPath());
        }
        Set<String> flagged = new HashSet<>();
        for (VerificationResult v : verificationRepository.findByWorkflowIdOrderByCreatedAt(c.getSourceWorkflowId())) {
            if (v.getStatus() == CheckStatus.FAIL || v.getStatus() == CheckStatus.WARN) {
                flagged.add(v.getTarget());
            }
        }

        return scorer.score(new EvaluationScorer.ScoreInput(
                expectedExtraction, actualExtraction, expectedMetrics, actualMetrics,
                expectedRisk, actualRisk, corrected, flagged));
    }

    // ---------------------------------------------------------- golden building
    private String buildGolden(WorkflowResponses.Detail detail, List<Correction> corrections) {
        Map<String, String> extraction = extractionMap(detail.results().extraction());
        Map<String, Double> metrics = metricsMap(detail.results().metrics());
        String risk = riskCategory(detail.results().risk());

        for (Correction c : corrections) {
            String path = c.getFieldPath();
            switch (c.getTargetType()) {
                case RISK_CATEGORY -> risk = c.getCorrectedValue();
                case METRIC -> {
                    Double d = parseDouble(c.getCorrectedValue());
                    if (d != null) metrics.put(path, d);
                }
                case EXTRACTION_FIELD -> extraction.put(path, c.getCorrectedValue());
                case MEMO -> { /* memo text not scored in Phase 2 */ }
            }
        }

        ObjectNode root = mapper.createObjectNode();
        ObjectNode ext = mapper.createObjectNode();
        extraction.forEach(ext::put);
        root.set("extraction", ext);
        ObjectNode met = mapper.createObjectNode();
        metrics.forEach(met::put);
        root.set("metrics", met);
        if (risk != null) root.put("risk", risk);
        return root.toString();
    }

    // ----------------------------------------------------------------- parsing
    private Map<String, String> extractionMap(JsonNode extraction) {
        Map<String, String> map = new HashMap<>();
        if (extraction == null) return map;
        for (String key : EXTRACTION_SCALARS) {
            JsonNode field = extraction.get(key);
            if (field != null && field.hasNonNull("value")) {
                map.put(key, field.get("value").asText());
            }
        }
        JsonNode financials = extraction.get("financials");
        if (financials != null) {
            for (String fk : FINANCIAL_KEYS) {
                if (financials.hasNonNull(fk)) {
                    map.put("financials." + fk, financials.get(fk).asText());
                }
            }
        }
        return map;
    }

    private Map<String, Double> metricsMap(JsonNode metrics) {
        Map<String, Double> map = new HashMap<>();
        if (metrics == null) return map;
        for (String mk : METRIC_KEYS) {
            JsonNode metric = metrics.get(mk);
            if (metric != null && metric.hasNonNull("value")) {
                map.put(mk, metric.get("value").asDouble());
            }
        }
        return map;
    }

    private String riskCategory(JsonNode risk) {
        return (risk != null && risk.hasNonNull("category")) ? risk.get("category").asText() : null;
    }

    private Map<String, String> stringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        if (node != null) {
            node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
        }
        return map;
    }

    private Map<String, Double> doubleMap(JsonNode node) {
        Map<String, Double> map = new HashMap<>();
        if (node != null) {
            node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asDouble()));
        }
        return map;
    }

    private JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String s) {
        try {
            return s == null ? null : Double.parseDouble(s.replaceAll("[,$%]", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
