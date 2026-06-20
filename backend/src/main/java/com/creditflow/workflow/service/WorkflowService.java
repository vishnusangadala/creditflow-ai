package com.creditflow.workflow.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.creditflow.agent.domain.AgentOutput;
import com.creditflow.agent.domain.AgentRun;
import com.creditflow.agent.domain.AgentType;
import com.creditflow.agent.domain.VerificationResult;
import com.creditflow.agent.repository.AgentOutputRepository;
import com.creditflow.agent.repository.AgentRunRepository;
import com.creditflow.agent.repository.VerificationResultRepository;
import com.creditflow.audit.domain.AuditEventType;
import com.creditflow.audit.service.AuditEventService;
import com.creditflow.common.Actor;
import com.creditflow.common.ResourceNotFoundException;
import com.creditflow.document.domain.Document;
import com.creditflow.document.repository.DocumentRepository;
import com.creditflow.document.service.DocumentStorageService;
import com.creditflow.workflow.api.WorkflowResponses;
import com.creditflow.workflow.domain.Workflow;
import com.creditflow.workflow.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Application service for workflows: accepting uploads, kicking off async
 * processing, and assembling the audit-complete detail view from the five
 * tables. Controllers stay thin; all coordination lives here.
 */
@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final AgentRunRepository agentRunRepository;
    private final AgentOutputRepository agentOutputRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final WorkflowProcessor workflowProcessor;
    private final AuditEventService audit;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository workflowRepository,
                           DocumentRepository documentRepository,
                           DocumentStorageService storageService,
                           AgentRunRepository agentRunRepository,
                           AgentOutputRepository agentOutputRepository,
                           VerificationResultRepository verificationResultRepository,
                           WorkflowProcessor workflowProcessor,
                           AuditEventService audit,
                           ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.agentRunRepository = agentRunRepository;
        this.agentOutputRepository = agentOutputRepository;
        this.verificationResultRepository = verificationResultRepository;
        this.workflowProcessor = workflowProcessor;
        this.audit = audit;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowResponses.Created createWorkflow(List<MultipartFile> files, Actor actor) {
        validate(files);

        Workflow workflow = Workflow.createNew();
        workflowRepository.save(workflow);

        for (MultipartFile file : files) {
            DocumentStorageService.StoredFile stored = storageService.store(workflow.getId(), file);
            Document document = Document.create(
                    workflow.getId(),
                    safeName(file.getOriginalFilename()),
                    file.getContentType(),
                    stored.sizeBytes(),
                    stored.storagePath());
            documentRepository.save(document);
        }

        workflow.markProcessing();
        workflowRepository.save(workflow);

        audit.record(workflow.getId(), AuditEventType.WORKFLOW_CREATED, actor.name(), actor.role(),
                "Uploaded " + files.size() + " document(s)", null);

        // Runs on the workflow executor; returns immediately.
        workflowProcessor.process(workflow.getId());

        return new WorkflowResponses.Created(workflow.getId(), workflow.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponses.Summary> listWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(w -> new WorkflowResponses.Summary(
                        w.getId(),
                        w.getStatus().name(),
                        w.getBorrowerName(),
                        w.getRiskCategory() == null ? null : w.getRiskCategory().name(),
                        w.getCreatedAt(),
                        w.getCompletedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponses.Detail getWorkflowDetail(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));

        List<Document> documents = documentRepository.findByWorkflowIdOrderByCreatedAt(id);
        List<AgentRun> runs = agentRunRepository.findByWorkflowIdOrderByStartedAt(id);
        List<VerificationResult> checks = verificationResultRepository.findByWorkflowIdOrderByCreatedAt(id);

        Map<UUID, JsonNode> outputsByRun = loadOutputs(runs);

        List<WorkflowResponses.AgentRunView> runViews = runs.stream()
                .map(r -> new WorkflowResponses.AgentRunView(
                        r.getId(),
                        r.getAgentType().name(),
                        r.getStatus().name(),
                        r.getModel(),
                        r.getLatencyMs(),
                        readTreeOrNull(r.getTokenUsage()),
                        r.getLangsmithRunId(),
                        r.getErrorMessage(),
                        r.getStartedAt(),
                        outputsByRun.get(r.getId())))
                .toList();

        WorkflowResponses.Results results = buildResults(runs, outputsByRun);

        return new WorkflowResponses.Detail(
                workflow.getId(),
                workflow.getStatus().name(),
                workflow.getBorrowerName(),
                workflow.getRiskCategory() == null ? null : workflow.getRiskCategory().name(),
                workflow.getErrorMessage(),
                workflow.getCreatedAt(),
                workflow.getCompletedAt(),
                documents.stream().map(this::toDocumentView).toList(),
                runViews,
                results,
                checks.stream().map(this::toCheckView).toList());
    }

    // --------------------------------------------------------------------- //
    // helpers
    // --------------------------------------------------------------------- //
    private Map<UUID, JsonNode> loadOutputs(List<AgentRun> runs) {
        Map<UUID, JsonNode> map = new HashMap<>();
        if (runs.isEmpty()) {
            return map;
        }
        List<UUID> runIds = runs.stream().map(AgentRun::getId).toList();
        for (AgentOutput output : agentOutputRepository.findByAgentRunIdIn(runIds)) {
            map.put(output.getAgentRunId(), readTreeOrNull(output.getOutput()));
        }
        return map;
    }

    private WorkflowResponses.Results buildResults(List<AgentRun> runs, Map<UUID, JsonNode> outputs) {
        Map<AgentType, JsonNode> byType = new HashMap<>();
        for (AgentRun run : runs) {
            JsonNode output = outputs.get(run.getId());
            if (output != null) {
                byType.putIfAbsent(run.getAgentType(), output);
            }
        }
        return new WorkflowResponses.Results(
                byType.get(AgentType.EXTRACTION),
                byType.get(AgentType.FINANCIAL_ANALYSIS),
                byType.get(AgentType.RISK_ASSESSMENT),
                byType.get(AgentType.CREDIT_MEMO));
    }

    private WorkflowResponses.DocumentView toDocumentView(Document d) {
        return new WorkflowResponses.DocumentView(
                d.getId(), d.getFilename(), d.getContentType(), d.getSizeBytes(), d.getPageCount());
    }

    private WorkflowResponses.CheckView toCheckView(VerificationResult v) {
        return new WorkflowResponses.CheckView(
                v.getCheckName(), v.getCategory(), v.getTarget(), v.getStatus().name(),
                v.getExpected(), v.getActual(), v.getEvidence());
    }

    private JsonNode readTreeOrNull(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void validate(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one document is required");
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Empty file uploaded: " + file.getOriginalFilename());
            }
            String name = file.getOriginalFilename();
            String type = file.getContentType();
            boolean looksPdf = (name != null && name.toLowerCase().endsWith(".pdf"))
                    || "application/pdf".equals(type);
            if (!looksPdf) {
                throw new IllegalArgumentException("Only PDF documents are supported: " + name);
            }
        }
    }

    private String safeName(String original) {
        return (original == null || original.isBlank()) ? "document.pdf" : original;
    }
}
