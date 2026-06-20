// Phase 2 API types: review/governance, evaluation, analytics, audit.
import type { WorkflowDetail } from "./index";

export type ActorRole = "ANALYST" | "REVIEWER" | "ADMIN";

export interface QueueItem {
  workflowId: string;
  reviewStatus: string;
  requiredReason: string | null;
  assignee: string | null;
  workflowStatus: string | null;
  borrowerName: string | null;
  riskCategory: string | null;
  createdAt: string;
}

export interface ReviewInfo {
  workflowId: string;
  status: string;
  requiredReason: string | null;
  assignee: string | null;
  decision: string | null;
  decisionReason: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
}

export interface CorrectionView {
  id: string;
  targetType: string;
  fieldPath: string;
  originalValue: string | null;
  correctedValue: string | null;
  failureCategory: string;
  note: string | null;
  correctedBy: string;
  createdAt: string;
}

export interface AuditView {
  eventType: string;
  actor: string;
  actorRole: string;
  summary: string | null;
  createdAt: string;
}

export interface ReviewDetail {
  review: ReviewInfo;
  workflow: WorkflowDetail;
  corrections: CorrectionView[];
  audit: AuditView[];
}

export interface CorrectionRequest {
  targetType: string;
  fieldPath: string;
  originalValue?: string | null;
  correctedValue?: string | null;
  failureCategory: string;
  note?: string | null;
}

// --- Evaluation ---
export interface DatasetView {
  id: string;
  name: string;
  description: string | null;
  caseCount: number;
  createdAt: string;
}

export interface CaseView {
  id: string;
  datasetId: string;
  name: string;
  sourceWorkflowId: string | null;
  expected: unknown;
  createdAt: string;
}

export interface RunView {
  id: string;
  datasetId: string;
  status: string;
  overallScore: number | null;
  scores: Record<string, number> | null;
  caseCount: number | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface CaseResultView {
  id: string;
  evalCaseId: string;
  score: number | null;
  dimensionScores: { dimension: string; score: number; detail: string }[] | null;
  createdAt: string;
}

// --- Analytics ---
export interface Analytics {
  totalWorkflows: number;
  workflowsByStatus: Record<string, number>;
  reviewsByStatus: Record<string, number>;
  reviewApprovalRate: number;
  avgLatencyMsByAgent: Record<string, number>;
  verificationByStatus: Record<string, number>;
  failureCategoryDistribution: Record<string, number>;
  totalTokens: number;
  evalTrend: { runId: string; datasetId: string; overallScore: number | null; finishedAt: string | null }[];
}

export interface AuditEventView {
  workflowId: string | null;
  eventType: string;
  actor: string;
  actorRole: string;
  summary: string | null;
  createdAt: string;
}
