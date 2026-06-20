-- CreditFlow AI — Phase 1 schema
-- Five tables matching the required entities: Workflows, Documents, Agent Runs,
-- Outputs, Verification Results. UUID PKs. Enums are stored as VARCHAR (mapped to
-- Java enums) rather than native PG enums for painless future migrations.
-- JSONB is used for flexible agent payloads.

CREATE TABLE workflows (
    id              UUID PRIMARY KEY,
    status          VARCHAR(32)  NOT NULL,
    borrower_name   VARCHAR(512),
    risk_category   VARCHAR(16),
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE documents (
    id              UUID PRIMARY KEY,
    workflow_id     UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    filename        VARCHAR(1024) NOT NULL,
    content_type    VARCHAR(255),
    size_bytes      BIGINT,
    storage_path    VARCHAR(2048),
    page_count      INTEGER,
    extracted_text  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_workflow ON documents(workflow_id);

-- Execution telemetry: one row per agent invocation (latency, tokens, model).
CREATE TABLE agent_runs (
    id                UUID PRIMARY KEY,
    workflow_id       UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    agent_type        VARCHAR(32) NOT NULL,
    status            VARCHAR(16) NOT NULL,
    model             VARCHAR(128),
    latency_ms        INTEGER,
    token_usage       JSONB,
    langsmith_run_id  VARCHAR(255),
    error_message     TEXT,
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at       TIMESTAMPTZ
);
CREATE INDEX idx_agent_runs_workflow ON agent_runs(workflow_id);

-- Produced artifact: the structured content an agent generated (1:1 with a run).
-- Kept separate from agent_runs so re-runs (Phase 2 learning loop) preserve history.
CREATE TABLE agent_outputs (
    id              UUID PRIMARY KEY,
    agent_run_id    UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    output          JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_agent_outputs_run ON agent_outputs(agent_run_id);

-- One row per verification check produced by the Verifier agent.
CREATE TABLE verification_results (
    id              UUID PRIMARY KEY,
    workflow_id     UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    agent_run_id    UUID REFERENCES agent_runs(id) ON DELETE SET NULL,
    check_name      VARCHAR(255) NOT NULL,
    category        VARCHAR(32)  NOT NULL,
    target          VARCHAR(512),
    status          VARCHAR(16)  NOT NULL,
    expected        TEXT,
    actual          TEXT,
    evidence        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_verification_workflow ON verification_results(workflow_id);
