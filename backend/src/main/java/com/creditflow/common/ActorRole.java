package com.creditflow.common;

/**
 * Demo-grade role model for Phase 2 governance. Supplied via the {@code X-Role}
 * header (no auth system yet — an honest placeholder). {@code SYSTEM} is used for
 * automated, policy-driven actions.
 */
public enum ActorRole {
    SYSTEM,
    ANALYST,
    REVIEWER,
    ADMIN
}
