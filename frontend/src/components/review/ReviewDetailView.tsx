import { useCallback, useEffect, useState } from "react";
import { assignReview, decideReview, getActor, getReview } from "../../api/client";
import type { ReviewDetail } from "../../types/phase2";
import { StatusBadge } from "../StatusBadge";
import { WorkflowDetailView } from "../WorkflowDetailView";
import { AuditTrail } from "./AuditTrail";
import { CorrectionForm } from "./CorrectionForm";

interface Props {
  workflowId: string;
  onBack: () => void;
}

const DECISIONS = [
  { key: "APPROVE", label: "Approve" },
  { key: "REQUEST_CHANGES", label: "Request changes" },
  { key: "REJECT", label: "Reject" },
];

export function ReviewDetailView({ workflowId, onBack }: Props) {
  const [detail, setDetail] = useState<ReviewDetail | null>(null);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const canDecide = getActor().role === "REVIEWER" || getActor().role === "ADMIN";

  const load = useCallback(() => {
    getReview(workflowId)
      .then(setDetail)
      .catch((e) => setError(e.message));
  }, [workflowId]);

  useEffect(() => {
    load();
  }, [load]);

  async function decide(decision: string) {
    setError(null);
    try {
      await decideReview(workflowId, decision, reason);
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Decision failed");
    }
  }

  async function assign() {
    try {
      await assignReview(workflowId);
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Assign failed");
    }
  }

  if (!detail) {
    return <div className="empty">Loading review…</div>;
  }

  const { review, corrections } = detail;

  return (
    <div className="detail">
      <header className="detail-header">
        <button className="link" onClick={onBack}>← back to queue</button>
        <StatusBadge status={review.status} />
      </header>

      <section className="card">
        <div className="row">
          <h3>Review · {detail.workflow.borrowerName ?? "Workflow"}</h3>
          {!review.decision && <button className="link" onClick={assign}>assign to me</button>}
        </div>
        <div className="small muted">
          Required: {review.requiredReason ?? "policy auto-approved"} · Assignee:{" "}
          {review.assignee ?? "unassigned"}
        </div>
        {review.decision && (
          <div className="notice notice-blue" style={{ marginTop: 10 }}>
            Decision: <strong>{review.decision}</strong>
            {review.decisionReason ? ` — ${review.decisionReason}` : ""} (by {review.decidedBy})
          </div>
        )}

        {!review.decision && (
          <div className="decision-panel">
            <input
              placeholder="Decision reason (optional)"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <div className="row">
              {DECISIONS.map((d) => (
                <button
                  key={d.key}
                  className="primary"
                  disabled={!canDecide}
                  onClick={() => decide(d.key)}
                >
                  {d.label}
                </button>
              ))}
            </div>
            {!canDecide && (
              <p className="small muted">
                Switch role to REVIEWER or ADMIN (top right) to make a decision.
              </p>
            )}
          </div>
        )}
        {error && <p className="error">{error}</p>}
      </section>

      <CorrectionForm workflowId={workflowId} onSaved={load} />

      {corrections.length > 0 && (
        <section className="card">
          <h3>Corrections ({corrections.length})</h3>
          <table className="check-table">
            <thead>
              <tr>
                <th>Field</th>
                <th>Original</th>
                <th>Corrected</th>
                <th>Failure</th>
                <th>By</th>
              </tr>
            </thead>
            <tbody>
              {corrections.map((c) => (
                <tr key={c.id}>
                  <td className="small">{c.fieldPath}</td>
                  <td className="small">{c.originalValue ?? "—"}</td>
                  <td className="small">{c.correctedValue ?? "—"}</td>
                  <td><StatusBadge status="WARN" />{" "}<span className="small">{c.failureCategory}</span></td>
                  <td className="small">{c.correctedBy}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <WorkflowDetailView detail={detail.workflow} />
      <AuditTrail events={detail.audit} />
    </div>
  );
}
