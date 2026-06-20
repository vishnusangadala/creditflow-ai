"""Credit memo agent — LLM synthesis, strictly grounded in prior outputs."""
from __future__ import annotations

from app.core.llm import call_structured
from app.domain.models import AgentType, CreditMemo
from app.graph.nodes._telemetry import failed_run, success_run, timed
from app.graph.state import WorkflowState
from app.prompts import MEMO_SYSTEM, MEMO_USER


def credit_memo_node(state: WorkflowState) -> WorkflowState:
    runs = state.get("agent_runs", [])
    extraction = state.get("extraction")
    metrics = state.get("metrics")
    risk = state.get("risk")
    with timed() as t:
        if extraction is None or metrics is None or risk is None:
            runs.append(failed_run(AgentType.CREDIT_MEMO, t["latency_ms"], "Missing inputs"))
            return {"agent_runs": runs, "error": "Credit memo skipped: missing inputs"}
        try:
            memo, telemetry = call_structured(
                CreditMemo,
                MEMO_SYSTEM,
                MEMO_USER.format(
                    extraction_json=extraction.model_dump_json(indent=2),
                    metrics_json=metrics.model_dump_json(indent=2),
                    risk_json=risk.model_dump_json(indent=2),
                ),
            )
        except Exception as exc:  # noqa: BLE001
            runs.append(failed_run(AgentType.CREDIT_MEMO, t["latency_ms"], str(exc)))
            return {"agent_runs": runs, "error": f"Credit memo failed: {exc}"}

    runs.append(success_run(AgentType.CREDIT_MEMO, t["latency_ms"], telemetry))
    return {"memo": memo, "agent_runs": runs}
