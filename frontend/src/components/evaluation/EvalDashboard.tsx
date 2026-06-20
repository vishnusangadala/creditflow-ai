import { useCallback, useEffect, useState } from "react";
import {
  createDataset,
  listDatasets,
  listRuns,
  promoteWorkflow,
  runEvaluation,
} from "../../api/client";
import type { DatasetView, RunView } from "../../types/phase2";
import { ScoreBars } from "./ScoreBars";

export function EvalDashboard() {
  const [datasets, setDatasets] = useState<DatasetView[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [runs, setRuns] = useState<RunView[]>([]);
  const [name, setName] = useState("");
  const [desc, setDesc] = useState("");
  const [promoteId, setPromoteId] = useState("");
  const [caseName, setCaseName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refreshDatasets = useCallback(() => {
    listDatasets().then(setDatasets).catch((e) => setError(e.message));
  }, []);

  const refreshRuns = useCallback((id: string) => {
    listRuns(id).then(setRuns).catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    refreshDatasets();
  }, [refreshDatasets]);

  useEffect(() => {
    if (selected) refreshRuns(selected);
  }, [selected, refreshRuns]);

  async function create() {
    if (!name) return;
    await createDataset(name, desc);
    setName("");
    setDesc("");
    refreshDatasets();
  }

  async function run() {
    if (!selected) return;
    setBusy(true);
    setError(null);
    try {
      await runEvaluation(selected);
      refreshRuns(selected);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Run failed");
    } finally {
      setBusy(false);
    }
  }

  async function promote() {
    if (!selected || !promoteId) return;
    setError(null);
    try {
      await promoteWorkflow(selected, promoteId.trim(), caseName);
      setPromoteId("");
      setCaseName("");
      refreshDatasets();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Promote failed");
    }
  }

  const latest = runs[0];

  return (
    <div className="eval">
      <div className="card">
        <h3>Evaluation datasets</h3>
        <p className="muted small">
          Promote a reviewed workflow into a dataset (its human-corrected values become the golden
          answer), then run an evaluation to score the agents — including whether the Verifier caught
          the errors humans found.
        </p>
        <div className="form-grid">
          <label>Name<input value={name} onChange={(e) => setName(e.target.value)} /></label>
          <label>Description<input value={desc} onChange={(e) => setDesc(e.target.value)} /></label>
        </div>
        <button className="primary" onClick={create} disabled={!name}>Create dataset</button>

        <ul className="dataset-list">
          {datasets.map((d) => (
            <li
              key={d.id}
              className={d.id === selected ? "selected" : ""}
              onClick={() => setSelected(d.id)}
            >
              <strong>{d.name}</strong>
              <span className="muted small"> · {d.caseCount} cases</span>
            </li>
          ))}
        </ul>
      </div>

      {selected && (
        <div className="card">
          <div className="row">
            <h3>Dataset</h3>
            <button className="primary" onClick={run} disabled={busy}>
              {busy ? "Running…" : "Run evaluation"}
            </button>
          </div>

          <div className="promote-box">
            <h4>Promote a workflow → golden case (learning loop)</h4>
            <div className="form-grid">
              <label>Workflow ID<input value={promoteId} onChange={(e) => setPromoteId(e.target.value)} /></label>
              <label>Case name<input value={caseName} onChange={(e) => setCaseName(e.target.value)} /></label>
            </div>
            <button className="link" onClick={promote} disabled={!promoteId}>promote</button>
          </div>

          {error && <p className="error">{error}</p>}

          {latest && (
            <div className="latest-run">
              <h4>
                Latest run · overall{" "}
                <span className="metric-value-inline">
                  {latest.overallScore != null ? latest.overallScore.toFixed(2) : "—"}
                </span>{" "}
                <span className="muted small">({latest.caseCount} cases)</span>
              </h4>
              {latest.scores && <ScoreBars scores={latest.scores} />}
            </div>
          )}

          {runs.length > 0 && (
            <table className="check-table">
              <thead>
                <tr><th>Run</th><th>Status</th><th>Overall</th><th>Cases</th><th>When</th></tr>
              </thead>
              <tbody>
                {runs.map((r) => (
                  <tr key={r.id}>
                    <td className="small">{r.id.slice(0, 8)}</td>
                    <td className="small">{r.status}</td>
                    <td className="small">{r.overallScore != null ? r.overallScore.toFixed(2) : "—"}</td>
                    <td className="small">{r.caseCount ?? "—"}</td>
                    <td className="small">
                      {r.finishedAt ? new Date(r.finishedAt).toLocaleString() : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
