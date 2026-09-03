import { BackendError } from "../components/BackendError";
import { CircuitBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDateTime, formatRelativeDays } from "../lib/format";
import type { SourceHealthEntry, SourceRegistryEntry } from "../lib/types";

export const dynamic = "force-dynamic";

export default async function SourcesPage() {
  let sources: SourceRegistryEntry[];
  let health: SourceHealthEntry[];
  try {
    [sources, health] = await Promise.all([
      fetchJson<SourceRegistryEntry[]>("/api/sources"),
      fetchJson<SourceHealthEntry[]>("/api/sources/health"),
    ]);
  } catch (error) {
    return (
      <>
        <h1>Sources</h1>
        <BackendError error={error} />
      </>
    );
  }

  const healthByCode = new Map(health.map((entry) => [entry.sourceCode, entry]));

  return (
    <>
      <h1>Sources</h1>
      <p className="page-subtitle">
        Runtime-available sources are queried by the backend itself. The rest arrive through the
        NDJSON import boundary, which is a statement about reachability, not about quality.
      </p>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Adapter</th>
              <th>Runtime</th>
              <th className="numeric">Trust</th>
              <th>Stable ids</th>
              <th>Full description</th>
              <th>Enabled</th>
            </tr>
          </thead>
          <tbody>
            {sources.map((source) => (
              <tr key={source.code}>
                <td>{source.code}</td>
                <td className="muted">{source.displayName}</td>
                <td className="muted">{source.adapterKind.toLowerCase().replace(/_/g, " ")}</td>
                <td>
                  <span className={`badge ${source.runtimeAvailable ? "badge-good" : "badge-unknown"}`}>
                    {source.runtimeAvailable ? "direct" : "import only"}
                  </span>
                </td>
                <td className="numeric">{source.trustTier}</td>
                <td className={source.stableExternalId ? "" : "unknown"}>
                  {source.stableExternalId ? "yes" : "no"}
                </td>
                <td className="muted">{source.providesFullDescription ? "yes" : "no"}</td>
                <td className="muted">{source.enabled ? "yes" : "no"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Health</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>Circuit</th>
              <th>Last success</th>
              <th>Last failure</th>
              <th className="numeric">Consecutive failures</th>
              <th className="numeric">Latency</th>
              <th className="numeric">Records last run</th>
              <th className="numeric">Rate limits</th>
              <th>Last error</th>
            </tr>
          </thead>
          <tbody>
            {health.map((entry) => (
              <tr key={entry.sourceCode}>
                <td>{entry.sourceCode}</td>
                <td>
                  <CircuitBadge state={entry.circuitState} />
                </td>
                <td className="muted">{formatRelativeDays(entry.lastSuccessAt)}</td>
                <td className="muted">{formatRelativeDays(entry.lastFailureAt)}</td>
                <td className="numeric">{entry.consecutiveFailures}</td>
                <td className="numeric muted">
                  {entry.rollingLatencyMs === null ? "-" : `${entry.rollingLatencyMs} ms`}
                </td>
                <td className="numeric">{entry.recordsLastRun}</td>
                <td className="numeric">{entry.rateLimitEvents}</td>
                <td className="muted">
                  <span className="evidence">{entry.lastError ?? "-"}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="pagination muted">
        Health last updated{" "}
        {health.length > 0
          ? formatDateTime(
              health
                .map((entry) => entry.updatedAt)
                .sort()
                .at(-1) ?? null,
            )
          : "never"}
        {healthByCode.size > 0 ? "" : ""}
      </p>
    </>
  );
}
