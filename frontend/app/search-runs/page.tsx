import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { StatusBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDateTime } from "../lib/format";
import type { PageResponse, SearchRun } from "../lib/types";

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
        <h1>Search runs</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>Search runs</h1>
      <p className="page-subtitle">
        {runs.totalElements} ingestion runs recorded. A failed run records against the run and the
        source, never against the jobs it could not see.
      </p>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Started</th>
              <th>Source</th>
              <th>Trigger</th>
              <th>Status</th>
              <th className="numeric">Received</th>
              <th className="numeric">Normalized</th>
              <th className="numeric">New</th>
              <th className="numeric">Updated</th>
              <th className="numeric">Duplicates</th>
              <th className="numeric">Closed</th>
              <th className="numeric">Failures</th>
              <th className="numeric">Duration</th>
              <th>Collector</th>
            </tr>
          </thead>
          <tbody>
            {runs.content.length === 0 ? (
              <tr>
                <td colSpan={13} className="muted">
                  No ingestion run has been recorded yet.
                </td>
              </tr>
            ) : (
              runs.content.map((run) => (
                <tr key={run.id}>
                  <td>
                    <Link href={`/search-runs/${run.id}`}>{formatDateTime(run.startedAt)}</Link>
                  </td>
                  <td>{run.sourceCode}</td>
                  <td className="muted">{run.triggerKind.toLowerCase()}</td>
                  <td>
                    <StatusBadge status={run.status} />
                  </td>
                  <td className="numeric">{run.recordsReceived}</td>
                  <td className="numeric">{run.recordsNormalized}</td>
                  <td className="numeric">{run.newJobs}</td>
                  <td className="numeric">{run.updatedJobs}</td>
                  <td className="numeric">{run.duplicates}</td>
                  <td className="numeric">{run.jobsClosed}</td>
                  <td className="numeric">{run.failures}</td>
                  <td className="numeric muted">
                    {run.durationMs === null ? "-" : `${run.durationMs} ms`}
                  </td>
                  <td className="muted">{run.collector ?? "-"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination">
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
    </>
  );
}
