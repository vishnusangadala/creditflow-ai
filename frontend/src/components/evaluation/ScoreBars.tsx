interface Props {
  scores: Record<string, number>;
}

// Renders each evaluation dimension as a labeled 0–1 bar.
export function ScoreBars({ scores }: Props) {
  const entries = Object.entries(scores);
  if (entries.length === 0) return null;
  return (
    <div className="score-bars">
      {entries.map(([dim, value]) => (
        <div className="score-row" key={dim}>
          <div className="score-label small">{dim.replace(/_/g, " ")}</div>
          <div className="score-track">
            <div
              className="score-fill"
              style={{ width: `${Math.round(value * 100)}%`, background: barColor(value) }}
            />
          </div>
          <div className="score-num small">{value.toFixed(2)}</div>
        </div>
      ))}
    </div>
  );
}

function barColor(v: number): string {
  if (v >= 0.8) return "var(--green)";
  if (v >= 0.5) return "var(--amber)";
  return "var(--red)";
}
