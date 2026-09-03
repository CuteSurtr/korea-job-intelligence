import { backendUrl } from "../lib/api";

export function BackendError({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : String(error);
  return (
    <div className="error-panel">
      <strong>The backend did not answer.</strong>
      <p className="muted">
        Expected it at <code>{backendUrl()}</code>. Start it with{" "}
        <code>docker compose up -d postgres redis</code> and{" "}
        <code>cd backend &amp;&amp; ./gradlew bootRun</code>.
      </p>
      <pre className="muted">{message}</pre>
    </div>
  );
}
