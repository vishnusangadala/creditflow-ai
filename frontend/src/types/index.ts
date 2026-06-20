// Types mirroring the backend API. Top-level fields are camelCase; the nested AI
// artifacts (extraction/metrics/risk/memo) keep the agent's snake_case schema and
// are typed loosely with optional fields so a partial run still renders.

export type WorkflowStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "NEEDS_REVIEW"
  | "FAILED";

export type RiskCategory = "LOW" | "MODERATE" | "HIGH";
export type CheckStatus = "PASS" | "WARN" | "FAIL";

export interface WorkflowSummary {
  id: string;
  status: WorkflowStatus;
  borrowerName: string | null;
  riskCategory: RiskCategory | null;
  createdAt: string;
  completedAt: string | null;
}

export interface DocumentView {
  id: string;
  filename: string;
  contentType: string | null;
  sizeBytes: number | null;
  pageCount: number | null;
}

export interface AgentRunView {
  id: string;
  agentType: string;
  status: "SUCCESS" | "FAILED";
  model: string | null;
  latencyMs: number | null;
  tokenUsage: Record<string, number> | null;
  langsmithRunId: string | null;
  errorMessage: string | null;
  startedAt: string;
  output: unknown;
}

export interface CheckView {
  checkName: string;
  category: string;
  target: string | null;
  status: CheckStatus;
  expected: string | null;
  actual: string | null;
  evidence: string | null;
}

// --- AI artifacts (snake_case, passed through verbatim) ---
export interface ExtractedField {
  value: string | null;
  evidence: string | null;
}

export interface Extraction {
  borrower?: ExtractedField;
  interest_rate?: ExtractedField;
  maturity_date?: ExtractedField;
  collateral?: ExtractedField;
  covenants?: string[];
  financials?: Record<string, unknown>;
}

export interface Metric {
  name: string;
  value: number | null;
  formula: string;
  inputs: Record<string, number | null>;
  note: string | null;
}

export interface Metrics {
  debt_to_ebitda: Metric;
  current_ratio: Metric;
  interest_coverage: Metric;
}

export interface Risk {
  category: RiskCategory;
  reasons: string[];
}

export interface Memo {
  executive_summary: string;
  financial_summary: string;
  risk_summary: string;
  recommendation: string;
}

export interface Results {
  extraction: Extraction | null;
  metrics: Metrics | null;
  risk: Risk | null;
  memo: Memo | null;
}

export interface WorkflowDetail {
  id: string;
  status: WorkflowStatus;
  borrowerName: string | null;
  riskCategory: RiskCategory | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  documents: DocumentView[];
  agentRuns: AgentRunView[];
  results: Results;
  verification: CheckView[];
}
