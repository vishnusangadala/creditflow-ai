-- CreditFlow AI — Phase 2 schema
-- Three subsystems on one backbone:
--   backbone : audit_events (append-only)
--   A review : reviews, corrections
--   B eval   : eval_datasets, eval_cases, eval_runs, eval_case_results
-- Same conventions as Phase 1: UUID PKs, VARCHAR enums, JSONB payloads.

-- --------------------------------------------------------------------------
-- Backbone: append-only audit trail. Every meaningful action writes one row.
-- --------------------------------------------------------------------------
CREATE TABLE audit_events (
    id            UUID PRIMARY KEY,
    workflow_id   UUID REFERENCES workflows(id) ON DELETE CASCADE,
    event_type    VARCHAR(48) NOT NULL,
    actor         VARCHAR(255) NOT NULL,
    actor_role    VARCHAR(16)  NOT NULL,
    summary       TEXT,
    payload       JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_workflow ON audit_events(workflow_id, created_at);
CREATE INDEX idx_audit_type ON audit_events(event_type);

-- --------------------------------------------------------------------------
-- A. Review & governance. One review per workflow; carries the human decision
--    separately from the workflow's AI status.
-- --------------------------------------------------------------------------
CREATE TABLE reviews (
    id               UUID PRIMARY KEY,
    workflow_id      UUID NOT NULL UNIQUE REFERENCES workflows(id) ON DELETE CASCADE,
    status           VARCHAR(24) NOT NULL,   -- PENDING, IN_REVIEW, APPROVED, REJECTED, CHANGES_REQUESTED
    required_reason  VARCHAR(48),            -- why review was required (policy)
    assignee         VARCHAR(255),
    decision         VARCHAR(24),            -- APPROVE, REJECT, REQUEST_CHANGES
    decision_reason  TEXT,
    decided_by       VARCHAR(255),
    decided_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_status ON reviews(status);

-- Human corrections — each is a labeled training signal for the learning loop.
CREATE TABLE corrections (
    id                UUID PRIMARY KEY,
    workflow_id       UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    review_id         UUID REFERENCES reviews(id) ON DELETE SET NULL,
    target_type       VARCHAR(32) NOT NULL,  -- EXTRACTION_FIELD, RISK_CATEGORY, METRIC, MEMO
    field_path        VARCHAR(255) NOT NULL, -- e.g. 'borrower', 'financials.ebitda', 'risk.category'
    original_value    TEXT,
    corrected_value   TEXT,
    failure_category  VARCHAR(32) NOT NULL,  -- taxonomy
    note              TEXT,
    corrected_by      VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_corrections_workflow ON corrections(workflow_id);
CREATE INDEX idx_corrections_failure ON corrections(failure_category);

-- --------------------------------------------------------------------------
-- B. Evaluation infrastructure + learning loop.
-- --------------------------------------------------------------------------
CREATE TABLE eval_datasets (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE eval_cases (
    id                  UUID PRIMARY KEY,
    dataset_id          UUID NOT NULL REFERENCES eval_datasets(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    source_workflow_id  UUID REFERENCES workflows(id) ON DELETE SET NULL,
    expected            JSONB NOT NULL,   -- golden answer
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_eval_cases_dataset ON eval_cases(dataset_id);

CREATE TABLE eval_runs (
    id             UUID PRIMARY KEY,
    dataset_id     UUID NOT NULL REFERENCES eval_datasets(id) ON DELETE CASCADE,
    status         VARCHAR(16) NOT NULL,   -- RUNNING, COMPLETED, FAILED
    overall_score  DOUBLE PRECISION,
    scores         JSONB,                  -- aggregated per-dimension scores
    case_count     INTEGER,
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ
);
CREATE INDEX idx_eval_runs_dataset ON eval_runs(dataset_id, started_at);

CREATE TABLE eval_case_results (
    id                UUID PRIMARY KEY,
    eval_run_id       UUID NOT NULL REFERENCES eval_runs(id) ON DELETE CASCADE,
    eval_case_id      UUID NOT NULL REFERENCES eval_cases(id) ON DELETE CASCADE,
    score             DOUBLE PRECISION,
    dimension_scores  JSONB,
    details           JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_eval_case_results_run ON eval_case_results(eval_run_id);
