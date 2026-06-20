import { useState } from "react";
import { createWorkflow } from "../api/client";

interface Props {
  onCreated: (id: string) => void;
}

export function UploadPanel({ onCreated }: Props) {
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (files.length === 0) return;
    setBusy(true);
    setError(null);
    try {
      const { id } = await createWorkflow(files);
      setFiles([]);
      onCreated(id);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Upload failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <h2>New analysis</h2>
      <p className="muted">Upload loan agreements or financial statements (PDF).</p>
      <input
        type="file"
        accept="application/pdf"
        multiple
        onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
      />
      {files.length > 0 && (
        <ul className="file-list">
          {files.map((f) => (
            <li key={f.name}>{f.name}</li>
          ))}
        </ul>
      )}
      <button className="primary" disabled={busy || files.length === 0} onClick={submit}>
        {busy ? "Uploading…" : "Run analysis"}
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
