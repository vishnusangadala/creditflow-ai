package com.creditflow.audit.domain;

/** The vocabulary of the append-only audit trail. */
public enum AuditEventType {
    WORKFLOW_CREATED,
    WORKFLOW_PROCESSED,
    REVIEW_CREATED,
    REVIEW_AUTO_APPROVED,
    REVIEW_ASSIGNED,
    FIELD_CORRECTED,
    DECISION_MADE,
    CASE_PROMOTED_TO_DATASET,
    EVAL_RUN_COMPLETED
}
