import type { WorkflowDetail, WorkflowSummary } from "../types";

// Relative base by default — the Vite dev server proxies /api to the backend.
// Override with VITE_API_BASE_URL for a deployed build.
const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

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

export async function listWorkflows(): Promise<WorkflowSummary[]> {
  return handle(await fetch(`${BASE}/api/v1/workflows`));
}

export async function getWorkflow(id: string): Promise<WorkflowDetail> {
  return handle(await fetch(`${BASE}/api/v1/workflows/${id}`));
}

export async function createWorkflow(files: File[]): Promise<{ id: string; status: string }> {
  const form = new FormData();
  files.forEach((f) => form.append("files", f));
  return handle(
    await fetch(`${BASE}/api/v1/workflows`, { method: "POST", body: form })
  );
}
