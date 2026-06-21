import type { WorkflowDetail } from "../types";
import { AgentRunsView } from "./AgentRunsView";
import { ExtractionView } from "./ExtractionView";
import { MemoView } from "./MemoView";
import { MetricsView } from "./MetricsView";
import { RiskView } from "./RiskView";
import { StatusBadge } from "./StatusBadge";
import { VerificationView } from "./VerificationView";

interface Props {
  detail: WorkflowDetail;
}

export function WorkflowDetailView({ detail }: Props) {
  const processing = detail.status === "PROCESSING" || detail.status === "PENDING";
  const { results } = detail;

  return (
    <div className="detail">
      <header className="detail-header">
        <h2>{detail.borrowerName ?? "Analysis"}</h2>
        <StatusBadge status={detail.status} />
      </header>

      {detail.status === "NEEDS_REVIEW" && (
        <div className="notice notice-amber">Held for review — verification flagged one or more checks.</div>
      )}
      {detail.status === "FAILED" && (
        <div className="notice notice-red">{detail.errorMessage ?? "Processing failed."}</div>
      )}
      {processing && <div className="notice notice-blue">Analyzing — results appear automatically.</div>}

      <section className="card">
        <h3>Documents</h3>
        <ul>
          {detail.documents.map((d) => (
            <li key={d.id}>
              {d.filename}
              {d.pageCount != null && <span className="muted small"> · {d.pageCount} pages</span>}
            </li>
          ))}
        </ul>
      </section>

      {results.extraction && <ExtractionView extraction={results.extraction} />}
      {results.metrics && <MetricsView metrics={results.metrics} />}
      {results.risk && <RiskView risk={results.risk} />}
      {results.memo && <MemoView memo={results.memo} />}
      <VerificationView checks={detail.verification} />
      <AgentRunsView runs={detail.agentRuns} />
    </div>
  );
}
