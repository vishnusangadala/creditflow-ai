import type { AgentRunView } from "../types";
import { StatusBadge } from "./StatusBadge";

interface Props {
  runs: AgentRunView[];
}

function prettyAgent(type: string): string {
  const s = type.replace(/_/g, " ").toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// The audit/observability strip: every agent invocation with its latency, model
// and token usage. Mirrors the agent_runs table.
export function AgentRunsView({ runs }: Props) {
  if (runs.length === 0) return null;
  return (
    <section className="card">
      <h3>Agent runs</h3>
      <table className="check-table">
        <thead>
          <tr>
            <th>Agent</th>
            <th>Status</th>
            <th>Model</th>
            <th>Latency</th>
            <th>Tokens</th>
          </tr>
        </thead>
        <tbody>
          {runs.map((r) => (
            <tr key={r.id}>
              <td>{prettyAgent(r.agentType)}</td>
              <td>
                <StatusBadge status={r.status} />
              </td>
              <td className="small">{r.model ?? "—"}</td>
              <td className="small">{r.latencyMs != null ? `${r.latencyMs} ms` : "—"}</td>
              <td className="small">{r.tokenUsage?.total_tokens ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
