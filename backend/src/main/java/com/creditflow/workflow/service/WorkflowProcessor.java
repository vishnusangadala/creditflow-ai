package com.creditflow.workflow.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.creditflow.agent.client.AgentRunRequest;
import com.creditflow.agent.client.AgentRunResult;
import com.creditflow.agent.client.AgentServiceClient;
import com.creditflow.agent.service.AgentResultPersister;
import com.creditflow.audit.domain.AuditEventType;
import com.creditflow.audit.service.AuditEventService;
import com.creditflow.common.ActorRole;
import com.creditflow.document.domain.Document;
import com.creditflow.document.repository.DocumentRepository;
import com.creditflow.document.service.DocumentStorageService;
import com.creditflow.review.service.ReviewService;
import com.creditflow.workflow.domain.RiskCategory;
import com.creditflow.workflow.domain.Workflow;
import com.creditflow.workflow.domain.WorkflowStatus;
import com.creditflow.workflow.repository.WorkflowRepository;

/**
 * Runs a workflow off the request thread: reads the stored documents, calls the
 * agent service, persists every agent run / output / verification, and writes
 * the terminal workflow status.
 *
 * <p>The whole multi-agent run happens inside one {@code @Async} method so the
 * HTTP upload can return {@code 202} immediately. A failure anywhere is captured
 * on the workflow as {@code FAILED} rather than lost.
 */
@Service
public class WorkflowProcessor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowProcessor.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final AgentServiceClient agentServiceClient;
    private final AgentResultPersister persister;
    private final WorkflowRepository workflowRepository;
    private final ReviewService reviewService;
    private final AuditEventService audit;

    public WorkflowProcessor(DocumentRepository documentRepository,
                             DocumentStorageService storageService,
                             AgentServiceClient agentServiceClient,
                             AgentResultPersister persister,
                             WorkflowRepository workflowRepository,
                             ReviewService reviewService,
                             AuditEventService audit) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.agentServiceClient = agentServiceClient;
        this.persister = persister;
        this.workflowRepository = workflowRepository;
        this.reviewService = reviewService;
        this.audit = audit;
    }

    @Async("workflowExecutor")
    public void process(UUID workflowId) {
        log.info("Processing workflow {}", workflowId);
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        try {
            List<Document> documents = documentRepository.findByWorkflowIdOrderByCreatedAt(workflowId);
            AgentRunRequest request = buildRequest(workflowId, documents);

            AgentRunResult result = agentServiceClient.run(request);

            persister.persist(workflowId, result);
            updateDocumentPageCounts(documents, result);

            WorkflowStatus status = WorkflowStatus.valueOf(result.status());
            RiskCategory risk = result.riskCategory() == null
                    ? null : RiskCategory.valueOf(result.riskCategory());
            workflow.complete(status, result.borrowerName(), risk);
            workflowRepository.save(workflow);

            audit.record(workflowId, AuditEventType.WORKFLOW_PROCESSED, "system", ActorRole.SYSTEM,
                    "Processing finished: " + status, null);
            // Governance gate: create the review (or auto-approve) for terminal outcomes.
            if (status == WorkflowStatus.COMPLETED || status == WorkflowStatus.NEEDS_REVIEW) {
                reviewService.createOnCompletion(workflowId, status, risk);
            }
            log.info("Workflow {} finished with status {}", workflowId, status);
        } catch (Exception e) {
            log.error("Workflow {} failed", workflowId, e);
            workflow.fail(e.getMessage());
            workflowRepository.save(workflow);
            audit.record(workflowId, AuditEventType.WORKFLOW_PROCESSED, "system", ActorRole.SYSTEM,
                    "Processing failed: " + e.getMessage(), null);
        }
    }

    private AgentRunRequest buildRequest(UUID workflowId, List<Document> documents) {
        List<AgentRunRequest.InputDocument> inputs = documents.stream()
                .map(d -> new AgentRunRequest.InputDocument(
                        d.getFilename(),
                        Base64.getEncoder().encodeToString(storageService.read(d.getStoragePath()))))
                .toList();
        return new AgentRunRequest(workflowId.toString(), inputs);
    }

    private void updateDocumentPageCounts(List<Document> documents, AgentRunResult result) {
        if (result.documents() == null) {
            return;
        }
        Map<String, Integer> pages = result.documents().stream()
                .filter(d -> d.pageCount() != null)
                .collect(Collectors.toMap(
                        AgentRunResult.ProcessedDocument::filename,
                        AgentRunResult.ProcessedDocument::pageCount,
                        (a, b) -> a));
        for (Document doc : documents) {
            Integer count = pages.get(doc.getFilename());
            if (count != null) {
                doc.setPageCount(count);
                documentRepository.save(doc);
            }
        }
    }
}
