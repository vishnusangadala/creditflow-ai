package com.creditflow.evaluation.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditflow.evaluation.domain.EvalCase;
import com.creditflow.evaluation.domain.EvalCaseResult;
import com.creditflow.evaluation.domain.EvalDataset;
import com.creditflow.evaluation.domain.EvalRun;
import com.creditflow.evaluation.repository.EvalCaseRepository;
import com.creditflow.evaluation.service.EvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** REST surface for the evaluation subsystem and the learning loop. */
@RestController
@RequestMapping("/api/v1/eval")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EvalCaseRepository caseRepository;
    private final ObjectMapper mapper;

    public EvaluationController(EvaluationService evaluationService, EvalCaseRepository caseRepository,
                               ObjectMapper mapper) {
        this.evaluationService = evaluationService;
        this.caseRepository = caseRepository;
        this.mapper = mapper;
    }

    @PostMapping("/datasets")
    public EvalResponses.DatasetView createDataset(@RequestBody EvalRequests.CreateDatasetRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Dataset name is required");
        }
        return toDatasetView(evaluationService.createDataset(req.name(), req.description()));
    }

    @GetMapping("/datasets")
    public List<EvalResponses.DatasetView> datasets() {
        return evaluationService.listDatasets().stream().map(this::toDatasetView).toList();
    }

    @GetMapping("/datasets/{datasetId}/cases")
    public List<EvalResponses.CaseView> cases(@PathVariable UUID datasetId) {
        return evaluationService.casesOf(datasetId).stream().map(this::toCaseView).toList();
    }

    @PostMapping("/datasets/{datasetId}/promote")
    public EvalResponses.CaseView promote(@PathVariable UUID datasetId,
                                          @RequestBody EvalRequests.PromoteRequest req) {
        if (req.workflowId() == null) {
            throw new IllegalArgumentException("workflowId is required");
        }
        return toCaseView(evaluationService.promote(datasetId, req.workflowId(), req.caseName()));
    }

    @PostMapping("/datasets/{datasetId}/run")
    public EvalResponses.RunView run(@PathVariable UUID datasetId) {
        return toRunView(evaluationService.run(datasetId));
    }

    @GetMapping("/datasets/{datasetId}/runs")
    public List<EvalResponses.RunView> runs(@PathVariable UUID datasetId) {
        return evaluationService.runsOf(datasetId).stream().map(this::toRunView).toList();
    }

    @GetMapping("/runs/{runId}/results")
    public List<EvalResponses.CaseResultView> results(@PathVariable UUID runId) {
        return evaluationService.resultsOf(runId).stream().map(this::toCaseResultView).toList();
    }

    // --------------------------------------------------------------------- //
    private EvalResponses.DatasetView toDatasetView(EvalDataset d) {
        return new EvalResponses.DatasetView(d.getId(), d.getName(), d.getDescription(),
                caseRepository.countByDatasetId(d.getId()), d.getCreatedAt());
    }

    private EvalResponses.CaseView toCaseView(EvalCase c) {
        return new EvalResponses.CaseView(c.getId(), c.getDatasetId(), c.getName(),
                c.getSourceWorkflowId(), readTree(c.getExpected()), c.getCreatedAt());
    }

    private EvalResponses.RunView toRunView(EvalRun r) {
        return new EvalResponses.RunView(r.getId(), r.getDatasetId(), r.getStatus().name(),
                r.getOverallScore(), readTree(r.getScores()), r.getCaseCount(),
                r.getStartedAt(), r.getFinishedAt());
    }

    private EvalResponses.CaseResultView toCaseResultView(EvalCaseResult r) {
        return new EvalResponses.CaseResultView(r.getId(), r.getEvalCaseId(), r.getScore(),
                readTree(r.getDimensionScores()), r.getCreatedAt());
    }

    private JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
