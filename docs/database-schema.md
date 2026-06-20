# Database Schema (Phase 1)

Five tables, matching the required entities. UUID primary keys; enums stored as
`VARCHAR` (mapped to Java enums) for painless migrations; `JSONB` for flexible
agent payloads. Source of truth: [`V1__init.sql`](../backend/src/main/resources/db/migration/V1__init.sql).

```mermaid
erDiagram
    workflows ||--o{ documents : has
    workflows ||--o{ agent_runs : has
    workflows ||--o{ verification_results : has
    agent_runs ||--|| agent_outputs : produces
    agent_runs ||--o{ verification_results : "verifier emits"

    workflows {
        uuid id PK
        varchar status
        varchar borrower_name
        varchar risk_category
        text error_message
        timestamptz created_at
        timestamptz completed_at
    }
    documents {
        uuid id PK
        uuid workflow_id FK
        varchar filename
        int page_count
        text extracted_text
    }
    agent_runs {
        uuid id PK
        uuid workflow_id FK
        varchar agent_type
        varchar status
        varchar model
        int latency_ms
        jsonb token_usage
        varchar langsmith_run_id
    }
    agent_outputs {
        uuid id PK
        uuid agent_run_id FK
        jsonb output
    }
    verification_results {
        uuid id PK
        uuid workflow_id FK
        uuid agent_run_id FK
        varchar check_name
        varchar category
        varchar status
        text evidence
    }
```

## Why `agent_runs` and `agent_outputs` are separate

`agent_runs` is **telemetry** (latency, tokens, model, success/failure).
`agent_outputs` is **content** (the structured artifact). Keeping them separate
means a re-run — Phase 2's "learning loop" — creates a new run + new output while
preserving the full history. An audit record is never overwritten.

## Status enums

- `workflows.status`: `PENDING · PROCESSING · COMPLETED · NEEDS_REVIEW · FAILED`
- `agent_runs.status`: `SUCCESS · FAILED`
- `verification_results.status`: `PASS · WARN · FAIL`
- `risk_category`: `LOW · MODERATE · HIGH`
