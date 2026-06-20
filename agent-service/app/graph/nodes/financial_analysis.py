"""Financial analysis agent — DETERMINISTIC. No LLM.

It reads the raw line items the extraction agent pulled and computes the credit
ratios in pure Python. This node exists as a graph step (so it shows up in the
audit trail / agent_runs) but its work is plain arithmetic.
"""
from __future__ import annotations

from app.domain.models import AgentType, Extraction
from app.graph.nodes._telemetry import failed_run, success_run, timed
from app.graph.state import WorkflowState
from app.services.financial import compute_metrics


def financial_analysis_node(state: WorkflowState) -> WorkflowState:
    runs = state.get("agent_runs", [])
    extraction: Extraction | None = state.get("extraction")
    with timed() as t:
        if extraction is None:
            runs.append(
                failed_run(AgentType.FINANCIAL_ANALYSIS, t["latency_ms"], "No extraction available")
            )
            return {"agent_runs": runs, "error": "Financial analysis skipped: no extraction"}
        metrics = compute_metrics(extraction.financials)

    runs.append(success_run(AgentType.FINANCIAL_ANALYSIS, t["latency_ms"]))
    return {"metrics": metrics, "agent_runs": runs}
