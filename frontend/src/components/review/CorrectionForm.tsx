import { useEffect, useState } from "react";
import { addCorrection, getFailureCategories } from "../../api/client";

interface Props {
  workflowId: string;
  onSaved: () => void;
}

const TARGET_TYPES = ["EXTRACTION_FIELD", "METRIC", "RISK_CATEGORY", "MEMO"];

// Lets a reviewer correct an agent output. The correction is the unit of the
// learning loop — tagged with a failure category for the taxonomy.
export function CorrectionForm({ workflowId, onSaved }: Props) {
  const [categories, setCategories] = useState<string[]>([]);
  const [targetType, setTargetType] = useState(TARGET_TYPES[0]);
  const [fieldPath, setFieldPath] = useState("");
  const [originalValue, setOriginalValue] = useState("");
  const [correctedValue, setCorrectedValue] = useState("");
  const [failureCategory, setFailureCategory] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getFailureCategories().then((c) => {
      setCategories(c);
      setFailureCategory(c[0] ?? "");
    });
  }, []);

  async function submit() {
    if (!fieldPath || !failureCategory) {
      setError("Field path and failure category are required");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await addCorrection(workflowId, {
        targetType,
        fieldPath,
        originalValue,
        correctedValue,
        failureCategory,
        note,
      });
      setFieldPath("");
      setOriginalValue("");
      setCorrectedValue("");
      setNote("");
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to save correction");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="correction-form">
      <h4>Add correction</h4>
      <div className="form-grid">
        <label>
          Target
          <select value={targetType} onChange={(e) => setTargetType(e.target.value)}>
            {TARGET_TYPES.map((t) => (
              <option key={t} value={t}>{t.replace(/_/g, " ")}</option>
            ))}
          </select>
        </label>
        <label>
          Field path
          <input
            placeholder="e.g. interest_rate, financials.ebitda, risk.category"
            value={fieldPath}
            onChange={(e) => setFieldPath(e.target.value)}
          />
        </label>
        <label>
          Original
          <input value={originalValue} onChange={(e) => setOriginalValue(e.target.value)} />
        </label>
        <label>
          Corrected
          <input value={correctedValue} onChange={(e) => setCorrectedValue(e.target.value)} />
        </label>
        <label>
          Failure category
          <select value={failureCategory} onChange={(e) => setFailureCategory(e.target.value)}>
            {categories.map((c) => (
              <option key={c} value={c}>{c.replace(/_/g, " ")}</option>
            ))}
          </select>
        </label>
        <label>
          Note
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </label>
      </div>
      <button className="primary" disabled={busy} onClick={submit}>
        {busy ? "Saving…" : "Save correction"}
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
