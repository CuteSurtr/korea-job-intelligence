import { notFound } from "next/navigation";
import { BackendError } from "../../components/BackendError";
import { StatusBadge } from "../../components/Badges";
import { fetchJson, isNotFound } from "../../lib/api";
import { formatDateTime } from "../../lib/format";
import type { SearchRun } from "../../lib/types";
import {
  Container,
  Fact,
  FactGrid,
  TableWrap,
  Td,
  Th,
} from "../../components/ui";

export const dynamic = "force-dynamic";

export default async function SearchRunDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let run: SearchRun;
  try {
    run = await fetchJson<SearchRun>(`/api/search-runs/${id}`);
  } catch (error) {
    if (isNotFound(error)) {
      notFound();
    }
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Search run {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="detail">
      <h1 className="text-2xl font-semibold tracking-tight">
        {run.sourceCode} run <StatusBadge status={run.status} />
      </h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">{run.runUuid}</p>

      <FactGrid>
        <Fact label="Trigger" value={run.triggerKind.toLowerCase()} />
        <Fact label="Query" value={run.queryText ?? "full listing"} />
        <Fact label="Collector" value={run.collector ?? "-"} />
        <Fact label="Started" value={formatDateTime(run.startedAt)} />
        <Fact label="Completed" value={formatDateTime(run.completedAt)} />
        <Fact label="Duration" value={run.durationMs === null ? "-" : `${run.durationMs} ms`} />
        <Fact label="Records received" value={String(run.recordsReceived)} />
        <Fact label="Records normalized" value={String(run.recordsNormalized)} />
        <Fact label="New jobs" value={String(run.newJobs)} />
        <Fact label="Updated jobs" value={String(run.updatedJobs)} />
        <Fact label="Duplicates merged" value={String(run.duplicates)} />
        <Fact label="Jobs closed" value={String(run.jobsClosed)} />
        <Fact label="Failures" value={String(run.failures)} />
        <Fact label="Rate limit events" value={String(run.rateLimitEvents)} />
      </FactGrid>

      {run.errorSummary ? (
        <>
          <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Error summary</h2>
          <div className="panel">
            <pre>{run.errorSummary}</pre>
          </div>
        </>
      ) : null}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Failures</h2>
      {run.failureDetails.length === 0 ? (
        <p className="text-muted-foreground">This run discarded nothing.</p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>When</Th>
                <Th>Stage</Th>
                <Th>Reason</Th>
                <Th>External id</Th>
                <Th>Message</Th>
              </tr>
            </thead>
            <tbody>
              {run.failureDetails.map((failure) => (
                <tr key={failure.id}>
                  <Td className="text-muted-foreground">{formatDateTime(failure.occurredAt)}</Td>
                  <Td>{failure.stage.toLowerCase().replace(/_/g, " ")}</Td>
                  <Td>{failure.reasonCode.toLowerCase().replace(/_/g, " ")}</Td>
                  <Td className="text-muted-foreground">{failure.externalId ?? "-"}</Td>
                  <Td>
                    <span className="evidence">{failure.message}</span>
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}
    </Container>
  );
}

