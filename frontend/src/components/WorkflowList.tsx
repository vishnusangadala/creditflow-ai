import type { WorkflowSummary } from "../types";
import { StatusBadge } from "./StatusBadge";

interface Props {
  workflows: WorkflowSummary[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

export function WorkflowList({ workflows, selectedId, onSelect }: Props) {
  return (
    <div className="panel">
      <h2>Workflows</h2>
      {workflows.length === 0 && <p className="muted">No analyses yet.</p>}
      <ul className="workflow-list">
        {workflows.map((w) => (
          <li
            key={w.id}
            className={w.id === selectedId ? "selected" : ""}
            onClick={() => onSelect(w.id)}
          >
            <div className="row">
              <strong>{w.borrowerName ?? "Untitled borrower"}</strong>
              <StatusBadge status={w.status} />
            </div>
            <div className="row muted small">
              <span>{new Date(w.createdAt).toLocaleString()}</span>
              {w.riskCategory && <StatusBadge status={w.riskCategory} />}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
