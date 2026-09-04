import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { StatusBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDateTime } from "../lib/format";
import type { PageResponse, SearchRun } from "../lib/types";
import {
  Container,
  TableWrap,
  Td,
  Th,
} from "../components/ui";

export const dynamic = "force-dynamic";

export default async function SearchRunsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const pageParam = params.page;
  const page = Number(Array.isArray(pageParam) ? pageParam[0] : (pageParam ?? "0"));

  let runs: PageResponse<SearchRun>;
  try {
    runs = await fetchJson<PageResponse<SearchRun>>("/api/search-runs", { page, size: 50 });
  } catch (error) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Search runs</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="table">
      <h1 className="text-2xl font-semibold tracking-tight">Search runs</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        {runs.totalElements} ingestion runs recorded. A failed run records against the run and the
        source, never against the jobs it could not see.
      </p>

      <TableWrap>
          <thead>
            <tr>
              <Th>Started</Th>
              <Th>Source</Th>
              <Th>Trigger</Th>
              <Th>Status</Th>
              <Th align="right">Received</Th>
              <Th align="right">Normalized</Th>
              <Th align="right">New</Th>
              <Th align="right">Updated</Th>
              <Th align="right">Duplicates</Th>
              <Th align="right">Closed</Th>
              <Th align="right">Failures</Th>
              <Th align="right">Duration</Th>
              <Th>Collector</Th>
            </tr>
          </thead>
          <tbody>
            {runs.content.length === 0 ? (
              <tr>
                <Td colSpan={13} className="text-muted-foreground">
                  No ingestion run has been recorded yet.
                </Td>
              </tr>
            ) : (
              runs.content.map((run) => (
                <tr key={run.id}>
                  <Td>
                    <Link href={`/search-runs/${run.id}`}>{formatDateTime(run.startedAt)}</Link>
                  </Td>
                  <Td>{run.sourceCode}</Td>
                  <Td className="text-muted-foreground">{run.triggerKind.toLowerCase()}</Td>
                  <Td>
                    <StatusBadge status={run.status} />
                  </Td>
                  <Td align="right">{run.recordsReceived}</Td>
                  <Td align="right">{run.recordsNormalized}</Td>
                  <Td align="right">{run.newJobs}</Td>
                  <Td align="right">{run.updatedJobs}</Td>
                  <Td align="right">{run.duplicates}</Td>
                  <Td align="right">{run.jobsClosed}</Td>
                  <Td align="right">{run.failures}</Td>
                  <Td align="right" className="text-muted-foreground">
                    {run.durationMs === null ? "-" : `${run.durationMs} ms`}
                  </Td>
                  <Td className="text-muted-foreground">{run.collector ?? "-"}</Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>

      <div className="mt-4 flex items-center gap-3 text-sm text-muted-foreground">
        {page > 0 ? (
          <Link href={`/search-runs?page=${page - 1}`}>Previous</Link>
        ) : (
          <span>Previous</span>
        )}
        <span>
          page {runs.page + 1} of {Math.max(1, runs.totalPages)}
        </span>
        {page + 1 < runs.totalPages ? (
          <Link href={`/search-runs?page=${page + 1}`}>Next</Link>
        ) : (
          <span>Next</span>
        )}
      </div>
    </Container>
  );
}
