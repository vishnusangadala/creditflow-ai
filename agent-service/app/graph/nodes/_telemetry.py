"""Helpers for building AgentRun telemetry records consistently across nodes."""
from __future__ import annotations

import time
from contextlib import contextmanager

from app.domain.models import AgentRun, AgentType, RunStatus


@contextmanager
def timed():
    start = time.perf_counter()
    box = {"latency_ms": 0}
    try:
        yield box
    finally:
        box["latency_ms"] = int((time.perf_counter() - start) * 1000)


def success_run(agent_type: AgentType, latency_ms: int, telemetry: dict | None = None) -> AgentRun:
    telemetry = telemetry or {}
    return AgentRun(
        agent_type=agent_type,
        status=RunStatus.SUCCESS,
        model=telemetry.get("model"),
        latency_ms=latency_ms,
        token_usage=telemetry.get("token_usage", {}),
        langsmith_run_id=telemetry.get("langsmith_run_id"),
    )


def failed_run(agent_type: AgentType, latency_ms: int, error: str) -> AgentRun:
    return AgentRun(
        agent_type=agent_type,
        status=RunStatus.FAILED,
        latency_ms=latency_ms,
        error_message=error,
    )
