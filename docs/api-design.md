# API Design (Phase 1)

## Public API (Spring Boot, `:8080`)

Base path: `/api/v1`

### `POST /api/v1/workflows`
Upload one or more PDFs and start the analysis.

- Request: `multipart/form-data`, field `files` (repeatable).
- Response `202 Accepted`:
  ```json
  { "id": "f1e2...", "status": "PROCESSING" }
  ```
- Errors: `400` if no files / non-PDF.

### `GET /api/v1/workflows`
List workflows, newest first.
```json
[
  { "id": "...", "status": "COMPLETED", "borrowerName": "Acme Corp",
    "riskCategory": "MODERATE", "createdAt": "...", "completedAt": "..." }
]
```

### `GET /api/v1/workflows/{id}`
Audit-complete detail. Top-level fields are camelCase; the nested AI artifacts
(`results.extraction`, `results.metrics`, ...) are passed through verbatim and keep
the agent's snake_case schema.
```json
{
  "id": "...", "status": "NEEDS_REVIEW", "borrowerName": "Acme Corp",
  "riskCategory": "HIGH",
  "documents": [{ "filename": "loan.pdf", "pageCount": 12 }],
  "agentRuns": [
    { "agentType": "EXTRACTION", "status": "SUCCESS", "model": "gpt-4o-mini",
      "latencyMs": 2143, "tokenUsage": { "total_tokens": 1820 },
      "output": { "borrower": { "value": "Acme Corp", "evidence": "..." } } }
  ],
  "results": {
    "extraction": { "...": "..." },
    "metrics": { "debt_to_ebitda": { "value": 4.2, "formula": "total_debt / ebitda" } },
    "risk": { "category": "HIGH", "reasons": ["..."] },
    "memo": { "executive_summary": "...", "recommendation": "..." }
  },
  "verification": [
    { "checkName": "recompute:debt_to_ebitda", "category": "RECOMPUTATION",
      "status": "PASS", "expected": "4.2", "actual": "reported=4.2, recomputed=4.2" }
  ]
}
```

### `GET /actuator/health`
Liveness.

## Internal API (Python agent service, `:8000`)

### `POST /run`
Called only by the backend.
```json
// request
{ "workflow_id": "...", "documents": [{ "filename": "loan.pdf", "content_base64": "..." }] }
```
Returns the full structured result (`RunResponse` in
[`models.py`](../agent-service/app/domain/models.py)): `status`, `borrower_name`,
`risk_category`, `documents`, `agent_runs`, `extraction`, `metrics`, `risk`,
`memo`, `verification`.

### `GET /health`
Liveness.

## Error shape

All backend errors return a uniform body:
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Workflow not found: ..." }
```
