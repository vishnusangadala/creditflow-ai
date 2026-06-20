"""FastAPI application entrypoint for the CreditFlow AI agent service."""
from __future__ import annotations

import logging

from fastapi import FastAPI

from app.api.routes import router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

app = FastAPI(
    title="CreditFlow AI — Agent Service",
    description="Multi-agent credit document analysis workflow (LangGraph + OpenAI).",
    version="0.1.0",
)
app.include_router(router)


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "creditflow-agent", "status": "ok"}
