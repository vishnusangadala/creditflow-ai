import { useEffect, useState } from "react";
import { listReviews } from "../../api/client";
import type { QueueItem } from "../../types/phase2";
import { StatusBadge } from "../StatusBadge";

interface Props {
  onOpen: (workflowId: string) => void;
}

export function ReviewQueue({ onOpen }: Props) {
  const [items, setItems] = useState<QueueItem[]>([]);
  const [all, setAll] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const load = () =>
      listReviews(all)
        .then((r) => active && setItems(r))
        .catch((e) => active && setError(e.message));
    load();
    const t = setInterval(load, 4000);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, [all]);

  return (
    <div className="card">
      <div className="row">
        <h3>Review queue</h3>
        <label className="small muted">
          <input type="checkbox" checked={all} onChange={(e) => setAll(e.target.checked)} /> show decided
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {items.length === 0 && <p className="muted">Nothing waiting for review.</p>}
      {items.length > 0 && (
        <table className="check-table">
          <thead>
            <tr>
              <th>Borrower</th>
              <th>Review</th>
              <th>Reason</th>
              <th>Risk</th>
              <th>Assignee</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.workflowId}>
                <td>{i.borrowerName ?? "—"}</td>
                <td><StatusBadge status={i.reviewStatus} /></td>
                <td className="small">{i.requiredReason ?? "—"}</td>
                <td>{i.riskCategory ? <StatusBadge status={i.riskCategory} /> : "—"}</td>
                <td className="small">{i.assignee ?? "unassigned"}</td>
                <td>
                  <button className="link" onClick={() => onOpen(i.workflowId)}>open</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
