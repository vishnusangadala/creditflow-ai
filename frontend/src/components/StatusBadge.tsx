interface Props {
  status: string;
}

// Maps a status/risk/check string to a color class. One place to control all
// the status coloring across the app.
const COLOR: Record<string, string> = {
  PENDING: "badge-grey",
  PROCESSING: "badge-blue",
  COMPLETED: "badge-green",
  NEEDS_REVIEW: "badge-amber",
  FAILED: "badge-red",
  LOW: "badge-green",
  MODERATE: "badge-amber",
  HIGH: "badge-red",
  PASS: "badge-green",
  WARN: "badge-amber",
  FAIL: "badge-red",
  SUCCESS: "badge-green",
};

export function StatusBadge({ status }: Props) {
  const cls = COLOR[status] ?? "badge-grey";
  return <span className={`badge ${cls}`}>{status.replace(/_/g, " ")}</span>;
}
