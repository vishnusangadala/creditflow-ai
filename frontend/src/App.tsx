import { useCallback, useEffect, useState } from "react";
import { getWorkflow, listWorkflows } from "./api/client";
import { UploadPanel } from "./components/UploadPanel";
import { WorkflowDetailView } from "./components/WorkflowDetailView";
import { WorkflowList } from "./components/WorkflowList";
import type { WorkflowDetail, WorkflowSummary } from "./types";

const LIST_POLL_MS = 4000;
const DETAIL_POLL_MS = 3000;

export function App() {
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<WorkflowDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refreshList = useCallback(async () => {
    try {
      setWorkflows(await listWorkflows());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load workflows");
    }
  }, []);

  // Poll the workflow list continuously so statuses stay fresh.
  useEffect(() => {
    refreshList();
    const t = setInterval(refreshList, LIST_POLL_MS);
    return () => clearInterval(t);
  }, [refreshList]);

  // Poll the selected workflow while it is still being processed.
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
    const t = setInterval(load, DETAIL_POLL_MS);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, [selectedId]);

  return (
    <div className="app">
      <header className="app-header">
        <h1>CreditFlow AI</h1>
        <span className="muted">Agentic credit document analysis · verify before you trust</span>
      </header>

      {error && <div className="notice notice-red">{error}</div>}

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
    </div>
  );
}
