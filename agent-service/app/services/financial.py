"""Deterministic financial ratio engine.

This module contains ZERO LLM calls by design. Credit decisions hinge on these
numbers, so they must be reproducible, testable, and auditable. The Verifier
agent re-uses this exact function to independently recompute and catch drift.
"""
from __future__ import annotations

from typing import Optional

from app.domain.models import FinancialLineItems, Metric, Metrics


def _safe_divide(numerator: Optional[float], denominator: Optional[float]) -> tuple[Optional[float], Optional[str]]:
    """Return (value, note). Value is None when it cannot be computed, with a
    human-readable note explaining why — never a silent NaN or a thrown error."""
    if numerator is None or denominator is None:
        missing = []
        if numerator is None:
            missing.append("numerator")
        if denominator is None:
            missing.append("denominator")
        return None, f"Missing input(s): {', '.join(missing)}"
    if denominator == 0:
        return None, "Denominator is zero"
    return round(numerator / denominator, 4), None


def compute_metrics(f: FinancialLineItems) -> Metrics:
    """Compute the three Phase 1 credit metrics from raw line items."""
    debt_val, debt_note = _safe_divide(f.total_debt, f.ebitda)
    current_val, current_note = _safe_divide(f.current_assets, f.current_liabilities)

    # Interest coverage prefers EBIT; falls back to EBITDA when EBIT is absent.
    coverage_numerator = f.ebit if f.ebit is not None else f.ebitda
    coverage_basis = "EBIT" if f.ebit is not None else "EBITDA"
    coverage_val, coverage_note = _safe_divide(coverage_numerator, f.interest_expense)

    return Metrics(
        debt_to_ebitda=Metric(
            name="Debt / EBITDA",
            value=debt_val,
            formula="total_debt / ebitda",
            inputs={"total_debt": f.total_debt, "ebitda": f.ebitda},
            note=debt_note,
        ),
        current_ratio=Metric(
            name="Current Ratio",
            value=current_val,
            formula="current_assets / current_liabilities",
            inputs={
                "current_assets": f.current_assets,
                "current_liabilities": f.current_liabilities,
            },
            note=current_note,
        ),
        interest_coverage=Metric(
            name="Interest Coverage",
            value=coverage_val,
            formula=f"{coverage_basis} / interest_expense",
            inputs={
                coverage_basis.lower(): coverage_numerator,
                "interest_expense": f.interest_expense,
            },
            note=coverage_note,
        ),
    )
