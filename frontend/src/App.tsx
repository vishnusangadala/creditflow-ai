import { useCallback, useEffect, useState } from "react";
import { getActor, getWorkflow, listWorkflows, setActor } from "./api/client";
import { AnalyticsDashboard } from "./components/analytics/AnalyticsDashboard";
import { EvalDashboard } from "./components/evaluation/EvalDashboard";
import { ReviewDetailView } from "./components/review/ReviewDetailView";
import { ReviewQueue } from "./components/review/ReviewQueue";
import { UploadPanel } from "./components/UploadPanel";
import { WorkflowDetailView } from "./components/WorkflowDetailView";
import { WorkflowList } from "./components/WorkflowList";
import type { WorkflowDetail, WorkflowSummary } from "./types";

type View = "workflows" | "reviews" | "evaluation" | "analytics";

const NAV: { key: View; label: string }[] = [
  { key: "workflows", label: "Workflows" },
  { key: "reviews", label: "Review queue" },
  { key: "evaluation", label: "Evaluation" },
  { key: "analytics", label: "Analytics" },
];

const ROLES = [
  { name: "analyst", role: "ANALYST" },
  { name: "reviewer", role: "REVIEWER" },
  { name: "admin", role: "ADMIN" },
];

export function App() {
  const [view, setView] = useState<View>("workflows");
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<WorkflowDetail | null>(null);
  const [openReviewId, setOpenReviewId] = useState<string | null>(null);
  const [roleKey, setRoleKey] = useState(getActor().role);
  const [error, setError] = useState<string | null>(null);

  const refreshList = useCallback(async () => {
    try {
      setWorkflows(await listWorkflows());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load workflows");
    }
  }, []);

  useEffect(() => {
    refreshList();
    const t = setInterval(refreshList, 4000);
    return () => clearInterval(t);
  }, [refreshList]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }
    let active = true;
    const load = async () => {
      try {
        const d = await getWorkflow(selectedId);
        if (active) setDetail(d);
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : "Failed to load workflow");
      }
    };
    load();
    const t = setInterval(load, 3000);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, [selectedId]);

  function changeRole(role: string) {
    const r = ROLES.find((x) => x.role === role) ?? ROLES[0];
    setActor(r);
    setRoleKey(r.role);
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>CreditFlow AI</h1>
        <nav className="nav">
          {NAV.map((n) => (
            <button
              key={n.key}
              className={view === n.key ? "nav-btn active" : "nav-btn"}
              onClick={() => {
                setView(n.key);
                setOpenReviewId(null);
              }}
            >
              {n.label}
            </button>
          ))}
        </nav>
        <div className="role-switch">
          <span className="muted small">acting as</span>
          <select value={roleKey} onChange={(e) => changeRole(e.target.value)}>
            {ROLES.map((r) => (
              <option key={r.role} value={r.role}>{r.role}</option>
            ))}
          </select>
        </div>
      </header>

      {error && <div className="notice notice-red">{error}</div>}

      {view === "workflows" && (
        <div className="layout">
          <aside className="sidebar">
            <UploadPanel
              onCreated={(id) => {
                setSelectedId(id);
                refreshList();
              }}
            />
            <WorkflowList workflows={workflows} selectedId={selectedId} onSelect={setSelectedId} />
          </aside>
          <main className="content">
            {detail ? (
              <WorkflowDetailView detail={detail} />
            ) : (
              <div className="empty">Select or upload a workflow to begin.</div>
            )}
          </main>
        </div>
      )}

      {view === "reviews" && (
        <main className="content wide">
          {openReviewId ? (
            <ReviewDetailView workflowId={openReviewId} onBack={() => setOpenReviewId(null)} />
          ) : (
            <ReviewQueue onOpen={setOpenReviewId} />
          )}
        </main>
      )}

      {view === "evaluation" && (
        <main className="content wide">
          <EvalDashboard />
        </main>
      )}

      {view === "analytics" && (
        <main className="content wide">
          <AnalyticsDashboard />
        </main>
      )}
    </div>
  );
}
