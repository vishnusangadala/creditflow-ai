package com.creditflow.workflow.domain;

/** Lifecycle of a credit-analysis workflow. NEEDS_REVIEW is the Phase 2 hand-off. */
public enum WorkflowStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    NEEDS_REVIEW,
    FAILED
}
