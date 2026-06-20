package com.creditflow.review.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.creditflow.audit.domain.AuditEvent;
import com.creditflow.audit.service.AuditEventService;
import com.creditflow.common.Actor;
import com.creditflow.review.domain.Correction;
import com.creditflow.review.domain.CorrectionTargetType;
import com.creditflow.review.domain.FailureCategory;
import com.creditflow.review.domain.Review;
import com.creditflow.review.domain.ReviewDecision;
import com.creditflow.review.service.ReviewService;
import com.creditflow.workflow.api.WorkflowResponses;
import com.creditflow.workflow.service.WorkflowService;

/**
 * REST surface for the human-review subsystem. Composes the AI output
 * (from {@link WorkflowService}) with review state, corrections, and the audit
 * trail — the one screen a reviewer needs.
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final WorkflowService workflowService;
    private final AuditEventService auditService;

    public ReviewController(ReviewService reviewService, WorkflowService workflowService,
                            AuditEventService auditService) {
        this.reviewService = reviewService;
        this.workflowService = workflowService;
        this.auditService = auditService;
    }

    /** The queue. {@code ?all=true} includes already-decided reviews. */
    @GetMapping
    public List<ReviewResponses.QueueItem> queue(
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        List<Review> reviews = all ? reviewService.all() : reviewService.queue();
        Map<UUID, WorkflowResponses.Summary> byId = workflowService.listWorkflows().stream()
                .collect(Collectors.toMap(WorkflowResponses.Summary::id, Function.identity()));
        return reviews.stream().map(r -> {
            WorkflowResponses.Summary s = byId.get(r.getWorkflowId());
            return new ReviewResponses.QueueItem(
                    r.getWorkflowId(),
                    r.getStatus().name(),
                    r.getRequiredReason(),
                    r.getAssignee(),
                    s == null ? null : s.status(),
                    s == null ? null : s.borrowerName(),
                    s == null ? null : s.riskCategory(),
                    r.getCreatedAt());
        }).toList();
    }

    /** Controlled failure taxonomy for the UI dropdown. */
    @GetMapping("/failure-categories")
    public List<String> failureCategories() {
        return List.of(FailureCategory.values()).stream().map(Enum::name).toList();
    }

    @GetMapping("/{workflowId}")
    public ReviewResponses.Detail detail(@PathVariable UUID workflowId) {
        Review review = reviewService.getByWorkflow(workflowId);
        WorkflowResponses.Detail workflow = workflowService.getWorkflowDetail(workflowId);
        List<ReviewResponses.CorrectionView> corrections = reviewService.corrections(workflowId)
                .stream().map(this::toCorrectionView).toList();
        List<ReviewResponses.AuditView> audit = auditService.forWorkflow(workflowId)
                .stream().map(this::toAuditView).toList();
        return new ReviewResponses.Detail(toReviewInfo(review), workflow, corrections, audit);
    }

    @PostMapping("/{workflowId}/assign")
    public ReviewResponses.ReviewInfo assign(@PathVariable UUID workflowId,
                                             @RequestHeader(value = "X-Actor", required = false) String actor,
                                             @RequestHeader(value = "X-Role", required = false) String role) {
        return toReviewInfo(reviewService.assign(workflowId, Actor.of(actor, role)));
    }

    @PostMapping("/{workflowId}/corrections")
    public ReviewResponses.CorrectionView correct(@PathVariable UUID workflowId,
                                                  @RequestBody ReviewRequests.CorrectionRequest req,
                                                  @RequestHeader(value = "X-Actor", required = false) String actor,
                                                  @RequestHeader(value = "X-Role", required = false) String role) {
        Correction c = reviewService.addCorrection(
                workflowId,
                parse(CorrectionTargetType.class, req.targetType(), "targetType"),
                req.fieldPath(),
                req.originalValue(),
                req.correctedValue(),
                parse(FailureCategory.class, req.failureCategory(), "failureCategory"),
                req.note(),
                Actor.of(actor, role));
        return toCorrectionView(c);
    }

    @PostMapping("/{workflowId}/decision")
    public ReviewResponses.ReviewInfo decide(@PathVariable UUID workflowId,
                                             @RequestBody ReviewRequests.DecisionRequest req,
                                             @RequestHeader(value = "X-Actor", required = false) String actor,
                                             @RequestHeader(value = "X-Role", required = false) String role) {
        Review review = reviewService.decide(
                workflowId,
                parse(ReviewDecision.class, req.decision(), "decision"),
                req.reason(),
                Actor.of(actor, role));
        return toReviewInfo(review);
    }

    // --------------------------------------------------------------------- //
    private <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field);
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private ReviewResponses.ReviewInfo toReviewInfo(Review r) {
        return new ReviewResponses.ReviewInfo(
                r.getWorkflowId(), r.getStatus().name(), r.getRequiredReason(), r.getAssignee(),
                r.getDecision() == null ? null : r.getDecision().name(),
                r.getDecisionReason(), r.getDecidedBy(), r.getDecidedAt());
    }

    private ReviewResponses.CorrectionView toCorrectionView(Correction c) {
        return new ReviewResponses.CorrectionView(
                c.getId(), c.getTargetType().name(), c.getFieldPath(), c.getOriginalValue(),
                c.getCorrectedValue(), c.getFailureCategory().name(), c.getNote(),
                c.getCorrectedBy(), c.getCreatedAt());
    }

    private ReviewResponses.AuditView toAuditView(AuditEvent e) {
        return new ReviewResponses.AuditView(
                e.getEventType().name(), e.getActor(), e.getActorRole().name(),
                e.getSummary(), e.getCreatedAt());
    }
}
