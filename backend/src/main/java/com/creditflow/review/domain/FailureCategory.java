package com.creditflow.review.domain;

/**
 * Controlled failure taxonomy. Every human correction is tagged with one of
 * these, turning ad-hoc fixes into structured, aggregatable failure data that
 * feeds the evaluation dashboard and learning loop.
 */
public enum FailureCategory {
    HALLUCINATED_FIELD,
    MISSING_FIELD,
    WRONG_VALUE,
    WRONG_CALCULATION,
    MEMO_DRIFT,
    WRONG_RISK_CATEGORY,
    OCR_PARSING_ERROR,
    OTHER
}
