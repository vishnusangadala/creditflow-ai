"""LLM client factory and a thin structured-output helper.

We use LangChain's `with_structured_output` so every LLM agent returns a typed
Pydantic object instead of free text we'd have to parse. Temperature is pinned
to 0 for reproducibility. Token usage and the LangSmith run id are captured for
telemetry (persisted into the `agent_runs` table by the backend).
"""
from __future__ import annotations

import os
from typing import Type, TypeVar

from langchain_openai import ChatOpenAI
from pydantic import BaseModel

from app.core.config import get_settings

T = TypeVar("T", bound=BaseModel)


def _configure_langsmith() -> None:
    s = get_settings()
    if s.langchain_tracing_v2 and s.langchain_api_key:
        os.environ["LANGCHAIN_TRACING_V2"] = "true"
        os.environ["LANGCHAIN_API_KEY"] = s.langchain_api_key
        os.environ["LANGCHAIN_PROJECT"] = s.langchain_project


def get_chat_model() -> ChatOpenAI:
    _configure_langsmith()
    s = get_settings()
    return ChatOpenAI(
        model=s.openai_model,
        temperature=s.openai_temperature,
        api_key=s.openai_api_key,
    )


class StructuredResult(BaseModel):
    """Carries the parsed object plus the telemetry the backend persists."""

    class Config:
        arbitrary_types_allowed = True


def call_structured(schema: Type[T], system_prompt: str, user_prompt: str) -> tuple[T, dict]:
    """Invoke the LLM and coerce the answer into `schema`.

    Returns (parsed_object, telemetry) where telemetry holds model name, token
    usage, and the LangSmith run id when tracing is enabled.
    """
    model = get_chat_model()
    # Use function/tool calling rather than OpenAI's strict json_schema mode.
    # Our schemas intentionally use optional fields (a value is null when not found
    # in the document); strict mode rejects that by requiring every property to be
    # listed as required. Function-calling handles optional fields cleanly.
    structured = model.with_structured_output(
        schema, method="function_calling", include_raw=True
    )
    result = structured.invoke(
        [
            ("system", system_prompt),
            ("human", user_prompt),
        ]
    )

    parsed: T = result["parsed"]
    raw = result.get("raw")

    token_usage: dict[str, int] = {}
    run_id = None
    if raw is not None:
        meta = getattr(raw, "usage_metadata", None) or {}
        if meta:
            token_usage = {
                "input_tokens": int(meta.get("input_tokens", 0)),
                "output_tokens": int(meta.get("output_tokens", 0)),
                "total_tokens": int(meta.get("total_tokens", 0)),
            }
        response_meta = getattr(raw, "response_metadata", {}) or {}
        run_id = response_meta.get("id")

    telemetry = {
        "model": get_settings().openai_model,
        "token_usage": token_usage,
        "langsmith_run_id": run_id,
    }
    return parsed, telemetry
