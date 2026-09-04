import { BackendError } from "../components/BackendError";
import { CircuitBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDateTime, formatRelativeDays } from "../lib/format";
import type { SourceHealthEntry, SourceRegistryEntry } from "../lib/types";
import {
  Badge,
  Container,
  TableWrap,
  Td,
  Th,
} from "../components/ui";

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
        <h1 className="text-2xl font-semibold tracking-tight">Sources</h1>
        <BackendError error={error} />
      </>
    );
  }

  const healthByCode = new Map(health.map((entry) => [entry.sourceCode, entry]));

  return (
    <Container width="table">
      <h1 className="text-2xl font-semibold tracking-tight">Sources</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        Runtime-available sources are queried by the backend itself. The rest arrive through the
        NDJSON import boundary, which is a statement about reachability, not about quality.
      </p>

      <TableWrap>
          <thead>
            <tr>
              <Th>Code</Th>
              <Th>Name</Th>
              <Th>Adapter</Th>
              <Th>Runtime</Th>
              <Th align="right">Trust</Th>
              <Th>Stable ids</Th>
              <Th>Full description</Th>
              <Th>Enabled</Th>
            </tr>
          </thead>
          <tbody>
            {sources.map((source) => (
              <tr key={source.code}>
                <Td>{source.code}</Td>
                <Td className="text-muted-foreground">{source.displayName}</Td>
                <Td className="text-muted-foreground">{source.adapterKind.toLowerCase().replace(/_/g, " ")}</Td>
                <Td>
                  <Badge tone={source.runtimeAvailable ? "good" : "neutral"}>
                    {source.runtimeAvailable ? "direct" : "import only"}
                  </Badge>
                </Td>
                <Td align="right">{source.trustTier}</Td>
                <Td className={source.stableExternalId ? "" : "text-muted-foreground/70"}>
                  {source.stableExternalId ? "yes" : "no"}
                </Td>
                <Td className="text-muted-foreground">{source.providesFullDescription ? "yes" : "no"}</Td>
                <Td className="text-muted-foreground">{source.enabled ? "yes" : "no"}</Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Health</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th>Source</Th>
              <Th>Circuit</Th>
              <Th>Last success</Th>
              <Th>Last failure</Th>
              <Th align="right">Consecutive failures</Th>
              <Th align="right">Latency</Th>
              <Th align="right">Records last run</Th>
              <Th align="right">Rate limits</Th>
              <Th>Last error</Th>
            </tr>
          </thead>
          <tbody>
            {health.map((entry) => (
              <tr key={entry.sourceCode}>
                <Td>{entry.sourceCode}</Td>
                <Td>
                  <CircuitBadge state={entry.circuitState} />
                </Td>
                <Td className="text-muted-foreground">{formatRelativeDays(entry.lastSuccessAt)}</Td>
                <Td className="text-muted-foreground">{formatRelativeDays(entry.lastFailureAt)}</Td>
                <Td align="right">{entry.consecutiveFailures}</Td>
                <Td align="right" className="text-muted-foreground">
                  {entry.rollingLatencyMs === null ? "-" : `${entry.rollingLatencyMs} ms`}
                </Td>
                <Td align="right">{entry.recordsLastRun}</Td>
                <Td align="right">{entry.rateLimitEvents}</Td>
                <Td className="text-muted-foreground">
                  <span className="evidence">{entry.lastError ?? "-"}</span>
                </Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

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
    </Container>
  );
}
