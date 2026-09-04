import { notFound } from "next/navigation";
import { BackendError } from "../../components/BackendError";
import { StatusBadge } from "../../components/Badges";
import { fetchJson, isNotFound } from "../../lib/api";
import { formatDateTime } from "../../lib/format";
import type { SearchRun } from "../../lib/types";

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
        <h1>Search run {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>
        {run.sourceCode} run <StatusBadge status={run.status} />
      </h1>
      <p className="page-subtitle">{run.runUuid}</p>

      <div className="detail-grid">
        <Field label="Trigger" value={run.triggerKind.toLowerCase()} />
        <Field label="Query" value={run.queryText ?? "full listing"} />
        <Field label="Collector" value={run.collector ?? "-"} />
        <Field label="Started" value={formatDateTime(run.startedAt)} />
        <Field label="Completed" value={formatDateTime(run.completedAt)} />
        <Field label="Duration" value={run.durationMs === null ? "-" : `${run.durationMs} ms`} />
        <Field label="Records received" value={String(run.recordsReceived)} />
        <Field label="Records normalized" value={String(run.recordsNormalized)} />
        <Field label="New jobs" value={String(run.newJobs)} />
        <Field label="Updated jobs" value={String(run.updatedJobs)} />
        <Field label="Duplicates merged" value={String(run.duplicates)} />
        <Field label="Jobs closed" value={String(run.jobsClosed)} />
        <Field label="Failures" value={String(run.failures)} />
        <Field label="Rate limit events" value={String(run.rateLimitEvents)} />
      </div>

      {run.errorSummary ? (
        <>
          <h2>Error summary</h2>
          <div className="panel">
            <pre>{run.errorSummary}</pre>
          </div>
        </>
      ) : null}

      <h2>Failures</h2>
      {run.failureDetails.length === 0 ? (
        <p className="muted">This run discarded nothing.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Stage</th>
                <th>Reason</th>
                <th>External id</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {run.failureDetails.map((failure) => (
                <tr key={failure.id}>
                  <td className="muted">{formatDateTime(failure.occurredAt)}</td>
                  <td>{failure.stage.toLowerCase().replace(/_/g, " ")}</td>
                  <td>{failure.reasonCode.toLowerCase().replace(/_/g, " ")}</td>
                  <td className="muted">{failure.externalId ?? "-"}</td>
                  <td>
                    <span className="evidence">{failure.message}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="label">{label}</div>
      <div>{value}</div>
    </div>
  );
}
