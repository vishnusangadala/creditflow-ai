package com.creditflow.review.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.creditflow.workflow.api.WorkflowResponses;

/** API response shapes for the review subsystem. */
public final class ReviewResponses {

    private ReviewResponses() {}

    /** A row in the review queue (review state joined with workflow facts). */
    public record QueueItem(
            UUID workflowId,
            String reviewStatus,
            String requiredReason,
            String assignee,
            String workflowStatus,
            String borrowerName,
            String riskCategory,
            Instant createdAt
    ) {}

    public record ReviewInfo(
            UUID workflowId,
            String status,
            String requiredReason,
            String assignee,
            String decision,
            String decisionReason,
            String decidedBy,
            Instant decidedAt
    ) {}

    public record CorrectionView(
            UUID id,
            String targetType,
            String fieldPath,
            String originalValue,
            String correctedValue,
            String failureCategory,
            String note,
            String correctedBy,
            Instant createdAt
    ) {}

    public record AuditView(
            String eventType,
            String actor,
            String actorRole,
            String summary,
            Instant createdAt
    ) {}

    /** The full review screen: the AI output, the human decision, corrections, trail. */
    public record Detail(
            ReviewInfo review,
            WorkflowResponses.Detail workflow,
            List<CorrectionView> corrections,
            List<AuditView> audit
    ) {}
}
