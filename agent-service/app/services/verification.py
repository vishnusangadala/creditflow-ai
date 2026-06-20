"""Deterministic verification checks.

These run WITHOUT an LLM. They are the part of the trust story that cannot
itself hallucinate:
  - grounding: does each extracted value's quote actually exist in the source?
  - recomputation: re-derive every ratio and compare to what was reported.

The LLM-based memo-alignment check lives in the verifier node and complements
these. Together they implement "never blindly trust AI".
"""
from __future__ import annotations

import re
from difflib import SequenceMatcher

from app.domain.models import (
    CheckStatus,
    Extraction,
    Metrics,
    VerificationCheck,
)
from app.services.financial import compute_metrics


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


def _is_grounded(quote: str | None, source: str, threshold: float = 0.85) -> tuple[bool, float]:
    """Return (grounded, best_ratio). Exact normalized substring -> grounded.
    Otherwise slide the quote across the source and take the best fuzzy ratio."""
    if not quote:
        return False, 0.0
    nq = _normalize(quote)
    ns = _normalize(source)
    if not nq:
        return False, 0.0
    if nq in ns:
        return True, 1.0

    window = len(nq)
    if window == 0 or window > len(ns):
        return False, SequenceMatcher(None, nq, ns).ratio()

    best = 0.0
    # step in chunks of a quarter window to keep this cheap on large documents
    step = max(1, window // 4)
    for i in range(0, len(ns) - window + 1, step):
        ratio = SequenceMatcher(None, nq, ns[i : i + window]).ratio()
        if ratio > best:
            best = ratio
            if best >= 1.0:
                break
    return best >= threshold, round(best, 3)


def grounding_checks(extraction: Extraction, source_text: str) -> list[VerificationCheck]:
    checks: list[VerificationCheck] = []

    field_map = {
        "borrower": extraction.borrower,
        "interest_rate": extraction.interest_rate,
        "maturity_date": extraction.maturity_date,
        "collateral": extraction.collateral,
    }
    for name, field in field_map.items():
        if field.value is None:
            continue  # nothing extracted -> nothing to ground
        grounded, ratio = _is_grounded(field.evidence, source_text)
        checks.append(
            VerificationCheck(
                check_name=f"grounding:{name}",
                category="GROUNDING",
                target=name,
                status=CheckStatus.PASS if grounded else CheckStatus.FAIL,
                expected="evidence quote present in source",
                actual=f"best match ratio {ratio}",
                evidence=field.evidence,
            )
        )

    # financial line items
    for item, quote in (extraction.financials.evidence or {}).items():
        grounded, ratio = _is_grounded(quote, source_text)
        checks.append(
            VerificationCheck(
                check_name=f"grounding:financials.{item}",
                category="GROUNDING",
                target=f"financials.{item}",
                status=CheckStatus.PASS if grounded else CheckStatus.WARN,
                expected="evidence quote present in source",
                actual=f"best match ratio {ratio}",
                evidence=quote,
            )
        )
    return checks


def recomputation_checks(extraction: Extraction, reported: Metrics) -> list[VerificationCheck]:
    """Independently recompute the ratios and compare to the reported metrics."""
    recomputed = compute_metrics(extraction.financials)
    checks: list[VerificationCheck] = []

    pairs = [
        ("debt_to_ebitda", reported.debt_to_ebitda, recomputed.debt_to_ebitda),
        ("current_ratio", reported.current_ratio, recomputed.current_ratio),
        ("interest_coverage", reported.interest_coverage, recomputed.interest_coverage),
    ]
    for name, rep, calc in pairs:
        if rep.value is None and calc.value is None:
            status = CheckStatus.PASS
            actual = "both null (insufficient inputs)"
        elif rep.value is None or calc.value is None:
            status = CheckStatus.WARN
            actual = f"reported={rep.value}, recomputed={calc.value}"
        else:
            match = abs(rep.value - calc.value) <= 0.01
            status = CheckStatus.PASS if match else CheckStatus.FAIL
            actual = f"reported={rep.value}, recomputed={calc.value}"
        checks.append(
            VerificationCheck(
                check_name=f"recompute:{name}",
                category="RECOMPUTATION",
                target=name,
                status=status,
                expected=f"{calc.value}",
                actual=actual,
                evidence=calc.formula,
            )
        )
    return checks


def overall_status(checks: list[VerificationCheck]) -> CheckStatus:
    if any(c.status == CheckStatus.FAIL for c in checks):
        return CheckStatus.FAIL
    if any(c.status == CheckStatus.WARN for c in checks):
        return CheckStatus.WARN
    return CheckStatus.PASS
