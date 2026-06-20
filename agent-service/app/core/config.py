"""Centralized settings, sourced from environment variables.

Twelve-factor style: no secrets in code. LangSmith tracing is opt-in and the
service runs fine without it (graceful degradation).
"""
from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # --- OpenAI ---
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    openai_temperature: float = 0.0  # determinism matters for credit analysis

    # --- LangSmith (observability) ---
    langchain_tracing_v2: bool = False
    langchain_api_key: str = ""
    langchain_project: str = "creditflow-ai"

    # --- Service ---
    app_host: str = "0.0.0.0"
    app_port: int = 8000


@lru_cache
def get_settings() -> Settings:
    return Settings()
