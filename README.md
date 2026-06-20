# CreditFlow AI

An early version of a financial AI operations platform: a **multi-agent workflow**
that reads credit documents, extracts facts, computes risk metrics, drafts a
credit memo — and then **verifies its own work** before a human is ever asked to
trust it.

> **Core principle:** never blindly trust AI.
> **Agent → Verification → (Human Review) → Approval.**

This repository is **Phase 1**: the core AI workflow, end to end. Phase 2 (human
review queue, audit trails, evaluation dashboard, governance) is scoped but not
built — see [the bottom of this file](#phase-2-not-yet-built).

---

## What it does

Upload one or more PDFs (loan agreements, financial statements). Five agents run
in sequence:

| # | Agent | Type | Produces |
|---|-------|------|----------|
| 1 | Extraction | LLM (+ evidence) | borrower, interest rate, maturity, collateral, covenants, financial line items — each with a source quote |
| 2 | Financial Analysis | **deterministic Python** | Debt/EBITDA, Current Ratio, Interest Coverage |
| 3 | Risk Assessment | LLM | risk category + reasons, grounded in the metrics |
| 4 | Credit Memo | LLM | executive / financial / risk summaries + recommendation |
| 5 | Verifier | **code + LLM** | grounding, recomputation, and memo-alignment checks |

If the Verifier flags anything, the workflow ends in **`NEEDS_REVIEW`** instead of
`COMPLETED` — the hand-off point for Phase 2's human queue.

## Architecture

```
React + TS  ──upload/poll──▶  Spring Boot (orchestrator + system of record)  ──JPA──▶  PostgreSQL
                                        │
                                        └──HTTP /run──▶  Python FastAPI + LangGraph  ──▶  OpenAI
                                                              (5 agents, LangSmith tracing)
```

Full write-ups: [architecture](docs/architecture.md) ·
[database schema](docs/database-schema.md) · [API design](docs/api-design.md).

## Tech stack

React · TypeScript · Java 21 · Spring Boot · PostgreSQL · Python · LangGraph ·
OpenAI · LangSmith · Docker.

## Folder structure

```
creditflow-ai/
├── docker-compose.yml          # full local stack
├── .env.example                # OPENAI_API_KEY etc.
├── docs/                       # architecture, schema, API design
├── backend/                    # Java 21 + Spring Boot
│   └── src/main/java/com/creditflow/
│       ├── workflow/  document/  agent/   # feature packages (api/domain/repository/service)
│       ├── config/    common/
│       └── src/main/resources/db/migration # Flyway V1__init.sql
├── agent-service/              # Python + FastAPI + LangGraph
│   └── app/
│       ├── graph/  (builder, state, nodes/)   # the 5-agent workflow
│       ├── services/ (financial, pdf, verification, runner)
│       ├── core/  domain/  prompts/  api/
└── frontend/                   # React + TypeScript (Vite)
    └── src/ (api/ types/ components/ styles/)
```

---

## Running it

### Option A — Docker (everything)

```bash
cp .env.example .env          # then put your real OPENAI_API_KEY in .env
docker compose up --build
```

- Frontend: <http://localhost:3000>
- Backend API: <http://localhost:8080/api/v1/workflows>
- Agent service: <http://localhost:8000/health>

### Option B — local dev (per service)

**1. Postgres**
```bash
docker compose up -d postgres
```

**2. Agent service** (Python 3.11+)
```bash
cd agent-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export OPENAI_API_KEY=sk-...
uvicorn app.main:app --reload --port 8000
```

**3. Backend** (Java 21)
```bash
cd backend
mvn spring-boot:run
# targets Java 21; if running on a newer JDK, byte-buddy may need:
#   MAVEN_OPTS=-Dnet.bytebuddy.experimental=true mvn spring-boot:run
```

**4. Frontend** (Node 20)
```bash
cd frontend
npm install
npm run dev     # http://localhost:5173 (proxies /api to :8080)
```

## Tests

```bash
# deterministic financial engine (no LLM, no network)
cd agent-service && pip install pydantic pytest && python -m pytest tests -q

# backend mapping logic
cd backend && mvn test
```

---

## Design decisions worth calling out

- **Math is never done by an LLM.** Ratios are pure Python (`services/financial.py`)
  and the Verifier *re-derives* them to catch drift.
- **Provenance is captured at extraction time** (a source quote per field). Without
  it, verification later would be impossible.
- **`agent_runs` (telemetry) is separate from `agent_outputs` (content)** so re-runs
  preserve history instead of overwriting audit records.
- **Async via Spring `@Async`, not a broker.** A real queue is a Phase 2 concern;
  adding it now would be premature.

## Phase 2 (not yet built)

Human Review Queue · Audit Trails · Evaluation Dashboard · Failure Taxonomy ·
Human Corrections · Learning Loop · Workflow Analytics · Governance · Advanced
Observability. The Phase 1 schema (`NEEDS_REVIEW` state, run/output separation,
verification records) is designed so Phase 2 is additive.
