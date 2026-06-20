import type { Risk } from "../types";
import { StatusBadge } from "./StatusBadge";

interface Props {
  risk: Risk;
}

export function RiskView({ risk }: Props) {
  return (
    <section className="card">
      <h3>
        Risk assessment <StatusBadge status={risk.category} />
      </h3>
      <ul className="reasons">
        {risk.reasons.map((r, i) => (
          <li key={i}>{r}</li>
        ))}
      </ul>
    </section>
  );
}
