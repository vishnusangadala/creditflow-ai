import type { Extraction } from "../types";

interface Props {
  extraction: Extraction;
}

function Field({ label, value, evidence }: { label: string; value?: string | null; evidence?: string | null }) {
  return (
    <div className="field">
      <div className="field-label">{label}</div>
      <div className="field-value">{value ?? <span className="muted">— not found —</span>}</div>
      {evidence && <div className="evidence">“{evidence}”</div>}
    </div>
  );
}

export function ExtractionView({ extraction }: Props) {
  return (
    <section className="card">
      <h3>Extracted facts</h3>
      <div className="field-grid">
        <Field label="Borrower" value={extraction.borrower?.value} evidence={extraction.borrower?.evidence} />
        <Field label="Interest rate" value={extraction.interest_rate?.value} evidence={extraction.interest_rate?.evidence} />
        <Field label="Maturity date" value={extraction.maturity_date?.value} evidence={extraction.maturity_date?.evidence} />
        <Field label="Collateral" value={extraction.collateral?.value} evidence={extraction.collateral?.evidence} />
      </div>
      {extraction.covenants && extraction.covenants.length > 0 && (
        <div className="field">
          <div className="field-label">Key covenants</div>
          <ul>
            {extraction.covenants.map((c, i) => (
              <li key={i}>{c}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
