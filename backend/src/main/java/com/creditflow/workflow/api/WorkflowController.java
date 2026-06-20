package com.creditflow.workflow.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.creditflow.common.Actor;
import com.creditflow.workflow.service.WorkflowService;

/**
 * REST surface for credit-analysis workflows. Thin by design — it delegates to
 * {@link WorkflowService} and only maps HTTP concerns.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /** Upload one or more PDFs and start the multi-agent analysis. Returns 202. */
    @PostMapping
    public ResponseEntity<WorkflowResponses.Created> create(
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role) {
        WorkflowResponses.Created created = workflowService.createWorkflow(files, Actor.of(actor, role));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
    }

    @GetMapping
    public List<WorkflowResponses.Summary> list() {
        return workflowService.listWorkflows();
    }

    @GetMapping("/{id}")
    public WorkflowResponses.Detail get(@PathVariable UUID id) {
        return workflowService.getWorkflowDetail(id);
    }
}
