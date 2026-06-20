import type { AuditView } from "../../types/phase2";

interface Props {
  events: AuditView[];
}

// The append-only audit trail for a workflow — the observability/governance record.
export function AuditTrail({ events }: Props) {
  if (events.length === 0) return null;
  return (
    <section className="card">
      <h3>Audit trail</h3>
      <ul className="timeline">
        {events.map((e, i) => (
          <li key={i}>
            <span className="timeline-dot" />
            <div>
              <div>
                <strong>{e.eventType.replace(/_/g, " ")}</strong>{" "}
                <span className="muted small">
                  by {e.actor} ({e.actorRole}) · {new Date(e.createdAt).toLocaleString()}
                </span>
              </div>
              {e.summary && <div className="small muted">{e.summary}</div>}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
