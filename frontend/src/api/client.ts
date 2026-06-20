import type { WorkflowDetail, WorkflowSummary } from "../types";
import type {
  Analytics,
  AuditEventView,
  CaseResultView,
  CaseView,
  CorrectionRequest,
  DatasetView,
  QueueItem,
  ReviewDetail,
  ReviewInfo,
  RunView,
} from "../types/phase2";

// Relative base by default — the Vite dev server proxies /api to the backend.
const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

// --- Actor context (demo-grade governance: who is acting, in what role) ---
export interface Actor {
  name: string;
  role: string;
}

let actor: Actor = loadActor();

function loadActor(): Actor {
  try {
    const raw = localStorage.getItem("cf_actor");
    if (raw) return JSON.parse(raw);
  } catch {
    /* ignore */
  }
  return { name: "analyst", role: "ANALYST" };
}

export function getActor(): Actor {
  return actor;
}

export function setActor(next: Actor): void {
  actor = next;
  localStorage.setItem("cf_actor", JSON.stringify(next));
}

function headers(extra?: Record<string, string>): Record<string, string> {
  return { "X-Actor": actor.name, "X-Role": actor.role, ...extra };
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message ?? detail;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(`${res.status}: ${detail}`);
  }
  return res.json() as Promise<T>;
}

// --- Workflows (Phase 1) ---
export async function listWorkflows(): Promise<WorkflowSummary[]> {
  return handle(await fetch(`${BASE}/api/v1/workflows`, { headers: headers() }));
}

export async function getWorkflow(id: string): Promise<WorkflowDetail> {
  return handle(await fetch(`${BASE}/api/v1/workflows/${id}`, { headers: headers() }));
}

export async function createWorkflow(files: File[]): Promise<{ id: string; status: string }> {
  const form = new FormData();
  files.forEach((f) => form.append("files", f));
  return handle(
    await fetch(`${BASE}/api/v1/workflows`, { method: "POST", body: form, headers: headers() })
  );
}

// --- Reviews / governance (Phase 2) ---
export async function listReviews(all = false): Promise<QueueItem[]> {
  return handle(await fetch(`${BASE}/api/v1/reviews?all=${all}`, { headers: headers() }));
}

export async function getReview(workflowId: string): Promise<ReviewDetail> {
  return handle(await fetch(`${BASE}/api/v1/reviews/${workflowId}`, { headers: headers() }));
}

export async function getFailureCategories(): Promise<string[]> {
  return handle(await fetch(`${BASE}/api/v1/reviews/failure-categories`, { headers: headers() }));
}

export async function assignReview(workflowId: string): Promise<ReviewInfo> {
  return handle(
    await fetch(`${BASE}/api/v1/reviews/${workflowId}/assign`, { method: "POST", headers: headers() })
  );
}

export async function addCorrection(workflowId: string, body: CorrectionRequest) {
  return handle(
    await fetch(`${BASE}/api/v1/reviews/${workflowId}/corrections`, {
      method: "POST",
      headers: headers({ "Content-Type": "application/json" }),
      body: JSON.stringify(body),
    })
  );
}

export async function decideReview(
  workflowId: string,
  decision: string,
  reason: string
): Promise<ReviewInfo> {
  return handle(
    await fetch(`${BASE}/api/v1/reviews/${workflowId}/decision`, {
      method: "POST",
      headers: headers({ "Content-Type": "application/json" }),
      body: JSON.stringify({ decision, reason }),
    })
  );
}

// --- Evaluation (Phase 2) ---
export async function listDatasets(): Promise<DatasetView[]> {
  return handle(await fetch(`${BASE}/api/v1/eval/datasets`, { headers: headers() }));
}

export async function createDataset(name: string, description: string): Promise<DatasetView> {
  return handle(
    await fetch(`${BASE}/api/v1/eval/datasets`, {
      method: "POST",
      headers: headers({ "Content-Type": "application/json" }),
      body: JSON.stringify({ name, description }),
    })
  );
}

export async function promoteWorkflow(
  datasetId: string,
  workflowId: string,
  caseName: string
): Promise<CaseView> {
  return handle(
    await fetch(`${BASE}/api/v1/eval/datasets/${datasetId}/promote`, {
      method: "POST",
      headers: headers({ "Content-Type": "application/json" }),
      body: JSON.stringify({ workflowId, caseName }),
    })
  );
}

export async function runEvaluation(datasetId: string): Promise<RunView> {
  return handle(
    await fetch(`${BASE}/api/v1/eval/datasets/${datasetId}/run`, { method: "POST", headers: headers() })
  );
}

export async function listRuns(datasetId: string): Promise<RunView[]> {
  return handle(await fetch(`${BASE}/api/v1/eval/datasets/${datasetId}/runs`, { headers: headers() }));
}

export async function getRunResults(runId: string): Promise<CaseResultView[]> {
  return handle(await fetch(`${BASE}/api/v1/eval/runs/${runId}/results`, { headers: headers() }));
}

// --- Analytics & audit (Phase 2) ---
export async function getAnalytics(): Promise<Analytics> {
  return handle(await fetch(`${BASE}/api/v1/analytics`, { headers: headers() }));
}

export async function getAuditFeed(): Promise<AuditEventView[]> {
  return handle(await fetch(`${BASE}/api/v1/audit`, { headers: headers() }));
}
