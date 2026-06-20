"""HTTP surface of the AI service. Intentionally thin — all logic lives in the
graph and services. The backend is the only caller."""
from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.domain.models import RunRequest, RunResponse
from app.services.runner import run_workflow

logger = logging.getLogger("creditflow.agent")
router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.post("/run", response_model=RunResponse)
def run(request: RunRequest) -> RunResponse:
    logger.info("Running workflow %s with %d document(s)", request.workflow_id, len(request.documents))
    try:
        return run_workflow(request)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Workflow %s crashed", request.workflow_id)
        raise HTTPException(status_code=500, detail=str(exc)) from exc
