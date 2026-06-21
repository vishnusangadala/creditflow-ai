# CreditFlow AI

A multi-agent system that reads loan documents, extracts the key terms, computes
the risk ratios, and drafts a credit memo. It checks its own output and sends
anything questionable to a human before approval.

## How it works

Each document runs through five agents:

1. **Extraction** (LLM) – borrower, interest rate, maturity, collateral, covenants
   and the raw financial figures. Every value keeps the quote it came from.
2. **Financial analysis** (plain Python) – Debt/EBITDA, current ratio, interest
   coverage. No LLM touches the math.
3. **Risk assessment** (LLM) – a risk category and reasons, based on the metrics.
4. **Credit memo** (LLM) – summary and recommendation.
5. **Verifier** – checks that extracted values actually appear in the document,
   re-computes the ratios, and compares the memo against the facts.

If the verifier flags something, the workflow ends in `NEEDS_REVIEW` and goes to a
reviewer. There's also a review queue, human corrections, an audit trail, and an
evaluation step that scores the agents against human-corrected cases.

## Stack

React + TypeScript, Java 21 + Spring Boot, PostgreSQL, Python + FastAPI +
LangGraph, OpenAI, LangSmith, Docker.

## Run it

Needs Docker and an OpenAI API key.

```bash
cp .env.example .env      # add your OPENAI_API_KEY
docker compose up --build
```

- App – http://localhost:3000
- API – http://localhost:8080
- Agents – http://localhost:8000

## Layout

```
backend/         Spring Boot – API, persistence, orchestration
agent-service/   Python – the LangGraph agents
frontend/        React UI
docs/            architecture, schema, API notes
```

## Tests

```bash
cd backend && mvn test
cd agent-service && pip install pydantic pytest && pytest tests
```

## Notes

- The ratios are computed in Python, and the verifier re-derives them, so the math
  is reproducible.
- Each extracted field stores its source quote, which is what makes verification
  possible.
- Processing happens in the background after upload; the UI polls for the result.

More detail in [`docs/`](docs/).
