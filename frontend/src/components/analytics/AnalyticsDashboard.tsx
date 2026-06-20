import { useEffect, useState } from "react";
import { getAnalytics } from "../../api/client";
import type { Analytics } from "../../types/phase2";

function Distribution({ title, data, suffix }: { title: string; data: Record<string, number>; suffix?: string }) {
  const entries = Object.entries(data);
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return (
    <div className="card">
      <h3>{title}</h3>
      {entries.length === 0 && <p className="muted small">No data yet.</p>}
      <div className="score-bars">
        {entries.map(([k, v]) => (
          <div className="score-row" key={k}>
            <div className="score-label small">{k.replace(/_/g, " ")}</div>
            <div className="score-track">
              <div className="score-fill" style={{ width: `${(v / max) * 100}%`, background: "var(--accent)" }} />
            </div>
            <div className="score-num small">{v}{suffix ?? ""}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function AnalyticsDashboard() {
  const [data, setData] = useState<Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const load = () =>
      getAnalytics()
        .then((d) => active && setData(d))
        .catch((e) => active && setError(e.message));
    load();
    const t = setInterval(load, 5000);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, []);

  if (error) return <div className="notice notice-red">{error}</div>;
  if (!data) return <div className="empty">Loading analytics…</div>;

  return (
    <div className="analytics">
      <div className="stat-grid">
        <Stat label="Workflows" value={data.totalWorkflows} />
        <Stat label="Approval rate" value={`${Math.round(data.reviewApprovalRate * 100)}%`} />
        <Stat label="Total tokens" value={data.totalTokens.toLocaleString()} />
        <Stat
          label="Verifier flags"
          value={(data.verificationByStatus.FAIL ?? 0) + (data.verificationByStatus.WARN ?? 0)}
        />
      </div>

      <div className="grid-2">
        <Distribution title="Workflows by status" data={data.workflowsByStatus} />
        <Distribution title="Reviews by status" data={data.reviewsByStatus} />
        <Distribution title="Verification checks by result" data={data.verificationByStatus} />
        <Distribution title="Failure taxonomy" data={data.failureCategoryDistribution} />
        <Distribution title="Avg latency by agent (ms)" data={roundValues(data.avgLatencyMsByAgent)} suffix="ms" />
      </div>

      {data.evalTrend.length > 0 && (
        <div className="card">
          <h3>Evaluation score trend</h3>
          <table className="check-table">
            <thead><tr><th>Run</th><th>Overall</th><th>When</th></tr></thead>
            <tbody>
              {data.evalTrend.map((p) => (
                <tr key={p.runId}>
                  <td className="small">{p.runId.slice(0, 8)}</td>
                  <td className="small">{p.overallScore != null ? p.overallScore.toFixed(2) : "—"}</td>
                  <td className="small">{p.finishedAt ? new Date(p.finishedAt).toLocaleString() : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="stat-card">
      <div className="stat-value">{value}</div>
      <div className="stat-label muted small">{label}</div>
    </div>
  );
}

function roundValues(m: Record<string, number>): Record<string, number> {
  return Object.fromEntries(Object.entries(m).map(([k, v]) => [k, Math.round(v)]));
}
