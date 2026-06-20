"""Risk assessment agent — LLM judgment grounded in the computed metrics."""
from __future__ import annotations

from app.core.llm import call_structured
from app.domain.models import AgentType, RiskAssessment
from app.graph.nodes._telemetry import failed_run, success_run, timed
from app.graph.state import WorkflowState
from app.prompts import RISK_SYSTEM, RISK_USER


def risk_assessment_node(state: WorkflowState) -> WorkflowState:
    runs = state.get("agent_runs", [])
    extraction = state.get("extraction")
    metrics = state.get("metrics")
    with timed() as t:
        if extraction is None or metrics is None:
            runs.append(failed_run(AgentType.RISK_ASSESSMENT, t["latency_ms"], "Missing inputs"))
            return {"agent_runs": runs, "error": "Risk assessment skipped: missing inputs"}
        try:
            risk, telemetry = call_structured(
                RiskAssessment,
                RISK_SYSTEM,
                RISK_USER.format(
                    extraction_json=extraction.model_dump_json(indent=2),
                    metrics_json=metrics.model_dump_json(indent=2),
                ),
            )
        except Exception as exc:  # noqa: BLE001
            runs.append(failed_run(AgentType.RISK_ASSESSMENT, t["latency_ms"], str(exc)))
            return {"agent_runs": runs, "error": f"Risk assessment failed: {exc}"}

    runs.append(success_run(AgentType.RISK_ASSESSMENT, t["latency_ms"], telemetry))
    return {"risk": risk, "agent_runs": runs}
