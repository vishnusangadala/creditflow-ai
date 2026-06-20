"""Orchestration entrypoint: takes a RunRequest, runs the graph, shapes the
RunResponse the backend persists.

This is where the workflow's terminal status is decided from the verification
outcome — the single source of truth for COMPLETED vs NEEDS_REVIEW.
"""
from __future__ import annotations

from app.domain.models import (
    CheckStatus,
    ProcessedDocument,
    RunRequest,
    RunResponse,
    WorkflowStatus,
)
from app.graph.builder import compiled_graph
from app.services.pdf import combine_text, parse_documents


def _terminal_status(error: str | None, verification) -> WorkflowStatus:
    if error:
        return WorkflowStatus.FAILED
    if verification is None:
        return WorkflowStatus.FAILED
    # A failed check means a human must look. WARN still completes but is visible.
    if verification.overall == CheckStatus.FAIL:
        return WorkflowStatus.NEEDS_REVIEW
    return WorkflowStatus.COMPLETED


def run_workflow(request: RunRequest) -> RunResponse:
    parsed = parse_documents(request.documents)
    source_text = combine_text(parsed)

    final_state = compiled_graph.invoke(
        {
            "workflow_id": request.workflow_id,
            "source_text": source_text,
            "agent_runs": [],
        }
    )

    extraction = final_state.get("extraction")
    verification = final_state.get("verification")
    risk = final_state.get("risk")
    error = final_state.get("error")

    status = _terminal_status(error, verification)

    borrower = None
    if extraction and extraction.borrower:
        borrower = extraction.borrower.value

    return RunResponse(
        workflow_id=request.workflow_id,
        status=status,
        borrower_name=borrower,
        risk_category=risk.category if risk else None,
        documents=[
            ProcessedDocument(
                filename=p.filename, page_count=p.page_count, char_count=len(p.text)
            )
            for p in parsed
        ],
        agent_runs=final_state.get("agent_runs", []),
        extraction=extraction,
        metrics=final_state.get("metrics"),
        risk=risk,
        memo=final_state.get("memo"),
        verification=verification,
        error_message=error,
    )
