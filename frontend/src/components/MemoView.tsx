import type { Memo } from "../types";

interface Props {
  memo: Memo;
}

export function MemoView({ memo }: Props) {
  return (
    <section className="card">
      <h3>Credit memo</h3>
      <h4>Executive summary</h4>
      <p>{memo.executive_summary}</p>
      <h4>Financial summary</h4>
      <p>{memo.financial_summary}</p>
      <h4>Risk summary</h4>
      <p>{memo.risk_summary}</p>
      <h4>Recommendation</h4>
      <p className="recommendation">{memo.recommendation}</p>
    </section>
  );
}
