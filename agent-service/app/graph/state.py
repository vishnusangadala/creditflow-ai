"""The state object threaded through the LangGraph workflow.

Each node reads what it needs and writes its own slice. Keeping a single typed
state (rather than passing tuples around) makes the graph readable and means
adding a node in Phase 2 is a localized change.
"""
from __future__ import annotations

from typing import Optional, TypedDict

from app.domain.models import (
    AgentRun,
    CreditMemo,
    Extraction,
    Metrics,
    RiskAssessment,
    Verification,
)


class WorkflowState(TypedDict, total=False):
    # inputs
    workflow_id: str
    source_text: str  # combined text of all uploaded documents

    # agent outputs (filled in as the graph progresses)
    extraction: Extraction
    metrics: Metrics
    risk: RiskAssessment
    memo: CreditMemo
    verification: Verification

    # telemetry — appended to by every node
    agent_runs: list[AgentRun]

    # control
    error: Optional[str]
