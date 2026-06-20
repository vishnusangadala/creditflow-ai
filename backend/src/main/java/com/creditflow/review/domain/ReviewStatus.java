package com.creditflow.review.domain;

/** Lifecycle of a human review, separate from the workflow's AI status. */
public enum ReviewStatus {
    PENDING,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CHANGES_REQUESTED
}
