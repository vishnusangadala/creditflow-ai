package com.creditflow.review.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditflow.audit.domain.AuditEventType;
import com.creditflow.audit.service.AuditEventService;
import com.creditflow.common.Actor;
import com.creditflow.common.ResourceNotFoundException;
import com.creditflow.review.domain.Correction;
import com.creditflow.review.domain.CorrectionTargetType;
import com.creditflow.review.domain.FailureCategory;
import com.creditflow.review.domain.Review;
import com.creditflow.review.domain.ReviewDecision;
import com.creditflow.review.domain.ReviewStatus;
import com.creditflow.review.repository.CorrectionRepository;
import com.creditflow.review.repository.ReviewRepository;
import com.creditflow.workflow.domain.RiskCategory;
import com.creditflow.workflow.domain.WorkflowStatus;

/**
 * Owns the review lifecycle and human corrections. Every state change also
 * writes an audit event, so the trail is complete by construction.
 *
 * <p>Deliberately does NOT depend on {@code WorkflowService} — the controller
 * composes the two — which keeps the bean graph acyclic
 * (WorkflowService → WorkflowProcessor → ReviewService).
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CorrectionRepository correctionRepository;
    private final GovernancePolicy policy;
    private final AuditEventService audit;

    public ReviewService(ReviewRepository reviewRepository,
                         CorrectionRepository correctionRepository,
                         GovernancePolicy policy,
                         AuditEventService audit) {
        this.reviewRepository = reviewRepository;
        this.correctionRepository = correctionRepository;
        this.policy = policy;
        this.audit = audit;
    }

    /**
     * Called when a workflow finishes. Creates the review (or auto-approves per
     * policy) and records the governance decision in the audit trail.
     */
    @Transactional
    public void createOnCompletion(UUID workflowId, WorkflowStatus status, RiskCategory risk) {
        if (reviewRepository.findByWorkflowId(workflowId).isPresent()) {
            return; // idempotent
        }
        String reason = policy.requiredReason(status, risk);
        if (reason != null) {
            reviewRepository.save(Review.pending(workflowId, reason));
            audit.record(workflowId, AuditEventType.REVIEW_CREATED, "system",
                    com.creditflow.common.ActorRole.SYSTEM,
                    "Human review required: " + reason, Map.of("reason", reason));
        } else {
            reviewRepository.save(Review.autoApproved(workflowId));
            audit.record(workflowId, AuditEventType.REVIEW_AUTO_APPROVED, "system",
                    com.creditflow.common.ActorRole.SYSTEM,
                    "Auto-approved by governance policy", null);
        }
    }

    @Transactional(readOnly = true)
    public List<Review> queue() {
        return reviewRepository.findByStatusInOrderByCreatedAt(
                List.of(ReviewStatus.PENDING, ReviewStatus.IN_REVIEW, ReviewStatus.CHANGES_REQUESTED));
    }

    @Transactional(readOnly = true)
    public List<Review> all() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Review getByWorkflow(UUID workflowId) {
        return reviewRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No review for workflow " + workflowId));
    }

    @Transactional(readOnly = true)
    public List<Correction> corrections(UUID workflowId) {
        return correctionRepository.findByWorkflowIdOrderByCreatedAt(workflowId);
    }

    @Transactional
    public Review assign(UUID workflowId, Actor actor) {
        Review review = getByWorkflow(workflowId);
        review.assign(actor.name());
        reviewRepository.save(review);
        audit.record(workflowId, AuditEventType.REVIEW_ASSIGNED, actor.name(), actor.role(),
                "Assigned to " + actor.name(), null);
        return review;
    }

    @Transactional
    public Correction addCorrection(UUID workflowId, CorrectionTargetType targetType, String fieldPath,
                                    String originalValue, String correctedValue,
                                    FailureCategory failureCategory, String note, Actor actor) {
        Review review = getByWorkflow(workflowId);
        Correction correction = Correction.create(workflowId, review.getId(), targetType, fieldPath,
                originalValue, correctedValue, failureCategory, note, actor.name());
        correctionRepository.save(correction);

        if (review.getStatus() == ReviewStatus.PENDING) {
            review.assign(actor.name()); // touching it starts the review
            reviewRepository.save(review);
        }
        audit.record(workflowId, AuditEventType.FIELD_CORRECTED, actor.name(), actor.role(),
                "Corrected " + fieldPath + " (" + failureCategory + ")",
                Map.of("fieldPath", fieldPath, "original", str(originalValue),
                        "corrected", str(correctedValue), "category", failureCategory.name()));
        return correction;
    }

    @Transactional
    public Review decide(UUID workflowId, ReviewDecision decision, String reason, Actor actor) {
        if (!actor.canDecide()) {
            throw new IllegalArgumentException(
                    "Role " + actor.role() + " is not permitted to make review decisions");
        }
        Review review = getByWorkflow(workflowId);
        review.decide(decision, reason, actor.name());
        reviewRepository.save(review);
        audit.record(workflowId, AuditEventType.DECISION_MADE, actor.name(), actor.role(),
                decision + (reason == null ? "" : " — " + reason),
                Map.of("decision", decision.name(), "reason", str(reason)));
        return review;
    }

    private String str(String s) {
        return s == null ? "" : s;
    }
}
