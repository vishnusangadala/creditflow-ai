import type { Metric, Metrics } from "../types";

interface Props {
  metrics: Metrics;
}

function MetricCard({ metric }: { metric: Metric }) {
  const display = metric.value !== null ? `${metric.value.toFixed(2)}×` : "N/A";
  return (
    <div className="metric-card">
      <div className="metric-name">{metric.name}</div>
      <div className="metric-value">
        {metric.value !== null ? display : <span className="muted">{display}</span>}
      </div>
      <div className="metric-formula muted small">{metric.formula}</div>
      {metric.note && <div className="metric-note small">{metric.note}</div>}
    </div>
  );
}

export function MetricsView({ metrics }: Props) {
  return (
    <section className="card">
      <h3>Financial metrics</h3>
      <div className="metric-grid">
        <MetricCard metric={metrics.debt_to_ebitda} />
        <MetricCard metric={metrics.current_ratio} />
        <MetricCard metric={metrics.interest_coverage} />
      </div>
    </section>
  );
}
