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
 * A single human correction to an agent output. Immutable once made. This is the
 * atom of the learning loop: {@code original_value -> corrected_value}, tagged
 * with a {@link FailureCategory}, becomes ground truth for evaluation.
 */
@Entity
@Table(name = "corrections")
public class Correction {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "review_id")
    private UUID reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private CorrectionTargetType targetType;

    @Column(name = "field_path", nullable = false, length = 255)
    private String fieldPath;

    @Column(name = "original_value")
    private String originalValue;

    @Column(name = "corrected_value")
    private String correctedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", nullable = false, length = 32)
    private FailureCategory failureCategory;

    @Column
    private String note;

    @Column(name = "corrected_by", nullable = false)
    private String correctedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Correction() {
        // for JPA
    }

    public static Correction create(UUID workflowId, UUID reviewId, CorrectionTargetType targetType,
                                    String fieldPath, String originalValue, String correctedValue,
                                    FailureCategory failureCategory, String note, String correctedBy) {
        Correction c = new Correction();
        c.id = UUID.randomUUID();
        c.workflowId = workflowId;
        c.reviewId = reviewId;
        c.targetType = targetType;
        c.fieldPath = fieldPath;
        c.originalValue = originalValue;
        c.correctedValue = correctedValue;
        c.failureCategory = failureCategory;
        c.note = note;
        c.correctedBy = correctedBy;
        c.createdAt = Instant.now();
        return c;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getReviewId() { return reviewId; }
    public CorrectionTargetType getTargetType() { return targetType; }
    public String getFieldPath() { return fieldPath; }
    public String getOriginalValue() { return originalValue; }
    public String getCorrectedValue() { return correctedValue; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public String getNote() { return note; }
    public String getCorrectedBy() { return correctedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
