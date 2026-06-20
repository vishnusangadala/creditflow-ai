package com.creditflow.review.service;

import org.springframework.stereotype.Service;

import com.creditflow.config.CreditFlowProperties;
import com.creditflow.workflow.domain.RiskCategory;
import com.creditflow.workflow.domain.WorkflowStatus;

/**
 * Decides whether a finished workflow needs a human sign-off, and why.
 *
 * <p>The policy is intentionally small and explicit (two rules). Keeping it in
 * one place means governance behaviour is auditable and changing it is a
 * one-method edit rather than a hunt through controllers.
 */
@Service
public class GovernancePolicy {

    private final CreditFlowProperties properties;

    public GovernancePolicy(CreditFlowProperties properties) {
        this.properties = properties;
    }

    /** Reason a review is required, or {@code null} when it can be auto-approved. */
    public String requiredReason(WorkflowStatus status, RiskCategory risk) {
        if (status == WorkflowStatus.NEEDS_REVIEW) {
            return "VERIFICATION_FAILED";
        }
        if (status == WorkflowStatus.COMPLETED
                && risk == RiskCategory.HIGH
                && properties.governance().requireReviewOnHighRisk()) {
            return "HIGH_RISK";
        }
        return null; // auto-approve
    }
}
