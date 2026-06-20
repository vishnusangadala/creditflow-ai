package com.creditflow.review.api;

/** Request bodies for the review endpoints. */
public final class ReviewRequests {

    private ReviewRequests() {}

    public record CorrectionRequest(
            String targetType,        // EXTRACTION_FIELD | METRIC | RISK_CATEGORY | MEMO
            String fieldPath,         // e.g. "interest_rate", "financials.ebitda", "risk.category"
            String originalValue,
            String correctedValue,
            String failureCategory,   // taxonomy
            String note
    ) {}

    public record DecisionRequest(
            String decision,          // APPROVE | REJECT | REQUEST_CHANGES
            String reason
    ) {}
}
