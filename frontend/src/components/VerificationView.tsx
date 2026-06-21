import type { CheckView } from "../types";
import { StatusBadge } from "./StatusBadge";

interface Props {
  checks: CheckView[];
}

// The trust surface: shows every verifier check grouped by category. This is the
// "never blindly trust AI" story made visible to the analyst.
export function VerificationView({ checks }: Props) {
  if (checks.length === 0) {
    return null;
  }
  const passed = checks.filter((c) => c.status === "PASS").length;
  const failed = checks.filter((c) => c.status === "FAIL").length;
  const warned = checks.filter((c) => c.status === "WARN").length;

  return (
    <section className="card">
      <h3>
        Verification{" "}
        <span className="muted small" style={{ fontWeight: 500, letterSpacing: "normal", textTransform: "none" }}>
          {passed} passed{warned ? ` · ${warned} warning${warned > 1 ? "s" : ""}` : ""}
          {failed ? ` · ${failed} failed` : ""}
        </span>
      </h3>
      <table className="check-table">
        <thead>
          <tr>
            <th>Status</th>
            <th>Category</th>
            <th>Target</th>
            <th>Detail</th>
          </tr>
        </thead>
        <tbody>
          {checks.map((c, i) => (
            <tr key={i}>
              <td>
                <StatusBadge status={c.status} />
              </td>
              <td className="small">{c.category}</td>
              <td className="small">{c.target}</td>
              <td className="small">
                {c.actual && <div>{c.actual}</div>}
                {c.evidence && <div className="evidence">{c.evidence}</div>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
