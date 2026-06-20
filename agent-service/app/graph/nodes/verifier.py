"""Verifier agent — the trust gate.

Runs three classes of check and aggregates them:
  1. GROUNDING      (deterministic) — extracted quotes exist in the source
  2. RECOMPUTATION  (deterministic) — ratios re-derived and compared
  3. MEMO_ALIGNMENT (LLM)           — memo prose matches the structured facts

If anything FAILs, the workflow is routed to NEEDS_REVIEW for a human.
"""
from __future__ import annotations

from pydantic import BaseModel, Field

from app.core.llm import call_structured
from app.domain.models import AgentType, Verification, VerificationCheck
from app.graph.nodes._telemetry import failed_run, success_run, timed
from app.graph.state import WorkflowState
from app.prompts import MEMO_ALIGNMENT_SYSTEM, MEMO_ALIGNMENT_USER
from app.services.verification import (
    grounding_checks,
    overall_status,
    recomputation_checks,
)


class _MemoAlignmentResult(BaseModel):
    """Structured-output wrapper for the LLM memo-alignment pass."""

    checks: list[VerificationCheck] = Field(default_factory=list)


def verifier_node(state: WorkflowState) -> WorkflowState:
    runs = state.get("agent_runs", [])
    extraction = state.get("extraction")
    metrics = state.get("metrics")
    risk = state.get("risk")
    memo = state.get("memo")

    with timed() as t:
        if extraction is None or metrics is None:
            runs.append(failed_run(AgentType.VERIFIER, t["latency_ms"], "Missing inputs"))
            return {"agent_runs": runs, "error": "Verification skipped: missing inputs"}

        checks: list[VerificationCheck] = []
        # 1 + 2: deterministic, cannot themselves hallucinate
        checks.extend(grounding_checks(extraction, state["source_text"]))
        checks.extend(recomputation_checks(extraction, metrics))

        telemetry = None
        # 3: LLM memo alignment (best-effort — a failure here degrades to WARN,
        # it must never crash the trust gate)
        if memo is not None and risk is not None:
            try:
                alignment, telemetry = call_structured(
                    _MemoAlignmentResult,
                    MEMO_ALIGNMENT_SYSTEM,
                    MEMO_ALIGNMENT_USER.format(
                        extraction_json=extraction.model_dump_json(indent=2),
                        metrics_json=metrics.model_dump_json(indent=2),
                        risk_json=risk.model_dump_json(indent=2),
                        memo_json=memo.model_dump_json(indent=2),
                    ),
                )
                checks.extend(alignment.checks)
            except Exception as exc:  # noqa: BLE001
                checks.append(
                    VerificationCheck(
                        check_name="memo_alignment:error",
                        category="MEMO_ALIGNMENT",
                        target="memo",
                        status="WARN",
                        actual=f"alignment check unavailable: {exc}",
                    )
                )

        verification = Verification(overall=overall_status(checks), checks=checks)

    runs.append(success_run(AgentType.VERIFIER, t["latency_ms"], telemetry))
    return {"verification": verification, "agent_runs": runs}
