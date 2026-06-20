package com.creditflow.review.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The human-review record for a workflow (1:1). Holds the decision separately
 * from the workflow's AI status so "what the AI produced" and "what the human
 * decided" never get tangled.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false, unique = true)
    private UUID workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReviewStatus status;

    @Column(name = "required_reason", length = 48)
    private String requiredReason;

    @Column
    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ReviewDecision decision;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {
        // for JPA
    }

    /** A review that needs a human (verification failed or policy required it). */
    public static Review pending(UUID workflowId, String requiredReason) {
        Review r = baseline(workflowId);
        r.status = ReviewStatus.PENDING;
        r.requiredReason = requiredReason;
        return r;
    }

    /** A review the governance policy auto-approved — recorded for a uniform trail. */
    public static Review autoApproved(UUID workflowId) {
        Review r = baseline(workflowId);
        r.status = ReviewStatus.APPROVED;
        r.decision = ReviewDecision.APPROVE;
        r.decisionReason = "Auto-approved by governance policy";
        r.decidedBy = "system";
        r.decidedAt = Instant.now();
        return r;
    }

    private static Review baseline(UUID workflowId) {
        Review r = new Review();
        r.id = UUID.randomUUID();
        r.workflowId = workflowId;
        Instant now = Instant.now();
        r.createdAt = now;
        r.updatedAt = now;
        return r;
    }

    public void assign(String assignee) {
        this.assignee = assignee;
        if (this.status == ReviewStatus.PENDING) {
            this.status = ReviewStatus.IN_REVIEW;
        }
        this.updatedAt = Instant.now();
    }

    public void decide(ReviewDecision decision, String reason, String decidedBy) {
        this.decision = decision;
        this.decisionReason = reason;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.status = switch (decision) {
            case APPROVE -> ReviewStatus.APPROVED;
            case REJECT -> ReviewStatus.REJECTED;
            case REQUEST_CHANGES -> ReviewStatus.CHANGES_REQUESTED;
        };
        this.updatedAt = this.decidedAt;
    }

    public boolean isDecided() {
        return decision != null;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public ReviewStatus getStatus() { return status; }
    public String getRequiredReason() { return requiredReason; }
    public String getAssignee() { return assignee; }
    public ReviewDecision getDecision() { return decision; }
    public String getDecisionReason() { return decisionReason; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
