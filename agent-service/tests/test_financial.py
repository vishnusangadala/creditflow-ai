"""Unit tests for the deterministic financial engine — the part that must never
be wrong. No LLM, no network: fast and reproducible."""
from app.domain.models import FinancialLineItems
from app.services.financial import compute_metrics


def test_basic_ratios():
    f = FinancialLineItems(
        total_debt=10_000_000,
        ebitda=4_000_000,
        ebit=3_500_000,
        current_assets=6_000_000,
        current_liabilities=3_000_000,
        interest_expense=1_000_000,
    )
    m = compute_metrics(f)
    assert m.debt_to_ebitda.value == 2.5
    assert m.current_ratio.value == 2.0
    # interest coverage prefers EBIT
    assert m.interest_coverage.value == 3.5
    assert "EBIT" in m.interest_coverage.formula


def test_interest_coverage_falls_back_to_ebitda():
    f = FinancialLineItems(ebitda=4_000_000, interest_expense=2_000_000)
    m = compute_metrics(f)
    assert m.interest_coverage.value == 2.0
    assert "EBITDA" in m.interest_coverage.formula


def test_divide_by_zero_is_null_with_note():
    f = FinancialLineItems(total_debt=5_000_000, ebitda=0)
    m = compute_metrics(f)
    assert m.debt_to_ebitda.value is None
    assert m.debt_to_ebitda.note == "Denominator is zero"


def test_missing_inputs_are_null_with_note():
    f = FinancialLineItems(total_debt=5_000_000)  # ebitda missing
    m = compute_metrics(f)
    assert m.debt_to_ebitda.value is None
    assert "Missing input" in m.debt_to_ebitda.note
