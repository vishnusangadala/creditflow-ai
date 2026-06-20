"""Shared contract between the AI service and the Spring Boot backend.

Every field the agents produce is defined here once. The backend has a
matching set of Java DTOs. Keeping this file small and explicit is what makes
the service boundary trustworthy: the JSON on the wire is exactly this shape.
"""
from __future__ import annotations

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


# --------------------------------------------------------------------------- #
# Enums (mirror the Java enums on the backend)
# --------------------------------------------------------------------------- #
class AgentType(str, Enum):
    EXTRACTION = "EXTRACTION"
    FINANCIAL_ANALYSIS = "FINANCIAL_ANALYSIS"
    RISK_ASSESSMENT = "RISK_ASSESSMENT"
    CREDIT_MEMO = "CREDIT_MEMO"
    VERIFIER = "VERIFIER"


class RunStatus(str, Enum):
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class RiskCategory(str, Enum):
    LOW = "LOW"
    MODERATE = "MODERATE"
    HIGH = "HIGH"


class CheckStatus(str, Enum):
    PASS = "PASS"
    WARN = "WARN"
    FAIL = "FAIL"


class WorkflowStatus(str, Enum):
    COMPLETED = "COMPLETED"
    NEEDS_REVIEW = "NEEDS_REVIEW"
    FAILED = "FAILED"


# --------------------------------------------------------------------------- #
# Extraction — every extracted value carries the source quote it came from.
# That `evidence` field is what makes the Verifier possible.
# --------------------------------------------------------------------------- #
class ExtractedField(BaseModel):
    value: Optional[str] = Field(None, description="The extracted value as written")
    evidence: Optional[str] = Field(
        None, description="Verbatim quote from the source document supporting the value"
    )


class FinancialLineItems(BaseModel):
    """Raw numbers read from the statements. NO ratios here — those are computed
    deterministically downstream. Each item keeps its supporting quote."""
    total_debt: Optional[float] = None
    ebitda: Optional[float] = None
    ebit: Optional[float] = None
    current_assets: Optional[float] = None
    current_liabilities: Optional[float] = None
    interest_expense: Optional[float] = None
    currency: Optional[str] = Field(None, description="e.g. USD")
    evidence: dict[str, str] = Field(
        default_factory=dict,
        description="Map of line-item name -> verbatim source quote",
    )


class Extraction(BaseModel):
    borrower: ExtractedField = Field(default_factory=ExtractedField)
    interest_rate: ExtractedField = Field(default_factory=ExtractedField)
    maturity_date: ExtractedField = Field(default_factory=ExtractedField)
    collateral: ExtractedField = Field(default_factory=ExtractedField)
    covenants: list[str] = Field(default_factory=list)
    covenants_evidence: list[str] = Field(default_factory=list)
    financials: FinancialLineItems = Field(default_factory=FinancialLineItems)


# --------------------------------------------------------------------------- #
# Financial metrics — computed in pure Python (services/financial.py)
# --------------------------------------------------------------------------- #
class Metric(BaseModel):
    name: str
    value: Optional[float] = None
    formula: str
    inputs: dict[str, Optional[float]] = Field(default_factory=dict)
    note: Optional[str] = Field(None, description="e.g. why value is null")


class Metrics(BaseModel):
    debt_to_ebitda: Metric
    current_ratio: Metric
    interest_coverage: Metric


# --------------------------------------------------------------------------- #
# Risk + Memo
# --------------------------------------------------------------------------- #
class RiskAssessment(BaseModel):
    category: RiskCategory
    reasons: list[str] = Field(default_factory=list)


class CreditMemo(BaseModel):
    executive_summary: str
    financial_summary: str
    risk_summary: str
    recommendation: str


# --------------------------------------------------------------------------- #
# Verification
# --------------------------------------------------------------------------- #
class VerificationCheck(BaseModel):
    check_name: str
    category: str = Field(description="GROUNDING | RECOMPUTATION | MEMO_ALIGNMENT")
    target: str = Field(description="The field / metric / claim being checked")
    status: CheckStatus
    expected: Optional[str] = None
    actual: Optional[str] = None
    evidence: Optional[str] = None


class Verification(BaseModel):
    overall: CheckStatus
    checks: list[VerificationCheck] = Field(default_factory=list)


# --------------------------------------------------------------------------- #
# Telemetry — one per agent, mirrors the `agent_runs` table
# --------------------------------------------------------------------------- #
class AgentRun(BaseModel):
    agent_type: AgentType
    status: RunStatus
    model: Optional[str] = None
    latency_ms: int = 0
    token_usage: dict[str, int] = Field(default_factory=dict)
    langsmith_run_id: Optional[str] = None
    error_message: Optional[str] = None


# --------------------------------------------------------------------------- #
# Request / Response envelopes
# --------------------------------------------------------------------------- #
class InputDocument(BaseModel):
    filename: str
    content_base64: str


class RunRequest(BaseModel):
    workflow_id: str
    documents: list[InputDocument]


class ProcessedDocument(BaseModel):
    filename: str
    page_count: int
    char_count: int


class RunResponse(BaseModel):
    workflow_id: str
    status: WorkflowStatus
    borrower_name: Optional[str] = None
    risk_category: Optional[RiskCategory] = None
    documents: list[ProcessedDocument] = Field(default_factory=list)
    agent_runs: list[AgentRun] = Field(default_factory=list)
    extraction: Optional[Extraction] = None
    metrics: Optional[Metrics] = None
    risk: Optional[RiskAssessment] = None
    memo: Optional[CreditMemo] = None
    verification: Optional[Verification] = None
    error_message: Optional[str] = None
