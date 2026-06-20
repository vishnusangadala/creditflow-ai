"""Extraction agent — LLM reads the documents and returns structured facts.

This is the one place where unstructured text becomes structured data. Every
value carries its supporting quote so the Verifier can ground-check it later.
"""
from __future__ import annotations

from app.core.llm import call_structured
from app.domain.models import AgentType, Extraction
from app.graph.nodes._telemetry import failed_run, success_run, timed
from app.graph.state import WorkflowState
from app.prompts import EXTRACTION_SYSTEM, EXTRACTION_USER


def extraction_node(state: WorkflowState) -> WorkflowState:
    runs = state.get("agent_runs", [])
    with timed() as t:
        try:
            extraction, telemetry = call_structured(
                Extraction,
                EXTRACTION_SYSTEM,
                EXTRACTION_USER.format(source_text=state["source_text"]),
            )
        except Exception as exc:  # noqa: BLE001 — surface as telemetry, fail the run
            runs.append(failed_run(AgentType.EXTRACTION, t["latency_ms"], str(exc)))
            return {"agent_runs": runs, "error": f"Extraction failed: {exc}"}

    runs.append(success_run(AgentType.EXTRACTION, t["latency_ms"], telemetry))
    return {"extraction": extraction, "agent_runs": runs}
