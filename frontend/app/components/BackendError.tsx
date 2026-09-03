import { backendUrl } from "../lib/api";

export function BackendError({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : String(error);
  const target = backendUrl();
  const isLocal = target.includes("localhost") || target.includes("127.0.0.1");

  return (
    <div className="error-panel">
      <strong>The console could not reach the API.</strong>
      <p className="muted">
        It tried <code>{target}</code>.
      </p>
      {isLocal ? (
        <p className="muted">
          Start the stack with <code>docker compose up -d</code>, or run the backend from
          source with <code>cd backend &amp;&amp; ./gradlew bootRun</code>.
        </p>
      ) : (
        <p className="muted">
          This console is only the front end. It needs a reachable deployment of the Spring
          Boot API and its PostgreSQL database. Point the <code>BACKEND_URL</code> environment
          variable at that deployment and redeploy.
        </p>
      )}
      <pre className="muted">{message}</pre>
    </div>
  );
}
