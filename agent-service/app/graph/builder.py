"""LangGraph wiring.

The Phase 1 pipeline is a linear chain. We keep a `should_continue` router so
that if any node sets `state["error"]`, the graph short-circuits to the end
instead of running downstream agents on bad inputs. This is a deliberate seam:
Phase 2 can add conditional branches (e.g. route to a human-review node) here
without touching the node implementations.
"""
from __future__ import annotations

from langgraph.graph import END, START, StateGraph

from app.graph.nodes.credit_memo import credit_memo_node
from app.graph.nodes.extraction import extraction_node
from app.graph.nodes.financial_analysis import financial_analysis_node
from app.graph.nodes.risk_assessment import risk_assessment_node
from app.graph.nodes.verifier import verifier_node
from app.graph.state import WorkflowState


def _continue_or_end(next_node: str):
    """Router: stop the chain early if an upstream node recorded an error."""

    def _router(state: WorkflowState) -> str:
        return END if state.get("error") else next_node

    return _router


def build_graph():
    graph = StateGraph(WorkflowState)

    graph.add_node("extraction", extraction_node)
    graph.add_node("financial_analysis", financial_analysis_node)
    graph.add_node("risk_assessment", risk_assessment_node)
    graph.add_node("credit_memo", credit_memo_node)
    graph.add_node("verifier", verifier_node)

    graph.add_edge(START, "extraction")
    graph.add_conditional_edges(
        "extraction", _continue_or_end("financial_analysis"), {"financial_analysis", END}
    )
    graph.add_conditional_edges(
        "financial_analysis", _continue_or_end("risk_assessment"), {"risk_assessment", END}
    )
    graph.add_conditional_edges(
        "risk_assessment", _continue_or_end("credit_memo"), {"credit_memo", END}
    )
    graph.add_conditional_edges("credit_memo", _continue_or_end("verifier"), {"verifier", END})
    graph.add_edge("verifier", END)

    return graph.compile()


# Compiled once at import time and reused across requests.
compiled_graph = build_graph()
