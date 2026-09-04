import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { Flash, WriteDisabled, param } from "../components/Flash";
import { OrUnknown, StatusBadge } from "../components/Badges";
import { setApplicationStatus } from "../lib/actions";
import { APPLICATION_STATUSES, statusLabel } from "../lib/applications";
import { canWrite, fetchJson } from "../lib/api";
import { formatDate, formatDateTime } from "../lib/format";
import type { Application, PageResponse } from "../lib/types";

export const dynamic = "force-dynamic";

export default async function ApplicationsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const status = param(params, "status") ?? "";
  const returnTo = status ? `/applications?status=${encodeURIComponent(status)}` : "/applications";
  const writable = canWrite();

  let applications: PageResponse<Application>;
  try {
    applications = await fetchJson<PageResponse<Application>>("/api/applications", {
      status,
      size: 100,
    });
  } catch (error) {
    return (
      <>
        <h1>Applications</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>Applications</h1>
      <p className="page-subtitle">
        {applications.totalElements} tracked applications. Every status change is recorded with the
        status it came from.
      </p>

      <Flash saved={param(params, "saved")} error={param(params, "error")} />
      <WriteDisabled />

      <form className="filters" method="get">
        <label>
          Status
          <select name="status" defaultValue={status}>
            <option value="">any</option>
            {APPLICATION_STATUSES.map((value) => (
              <option key={value} value={value}>
                {statusLabel(value)}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Apply</button>
        <Link className="badge reset" href="/applications">
          Reset
        </Link>
      </form>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Company</th>
              <th>Role</th>
              <th>Status</th>
              <th>Move to</th>
              <th>Applied</th>
              <th>Resume</th>
              <th>Interview stage</th>
              <th>Follow up</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {applications.content.length === 0 ? (
              <tr>
                <td colSpan={9} className="muted">
                  {status
                    ? `No application is ${statusLabel(status)}.`
                    : "No application has been recorded yet. Open a job and track it."}
                </td>
              </tr>
            ) : (
              applications.content.map((application) => (
                <tr key={application.id}>
                  <td className="muted">
                    <OrUnknown value={application.companyName} />
                  </td>
                  <td>
                    <Link href={`/applications/${application.id}`}>
                      <OrUnknown value={application.jobTitle} />
                    </Link>
                  </td>
                  <td>
                    <StatusBadge status={application.status} />
                  </td>
                  <td>
                    {/* Triage is almost always a status change and nothing else, so it happens
                        here rather than by opening every row. */}
                    <form action={setApplicationStatus} className="inline-form">
                      <input type="hidden" name="id" value={application.id} />
                      <input type="hidden" name="returnTo" value={returnTo} />
                      <label className="sr-only" htmlFor={`status-${application.id}`}>
                        Move {application.jobTitle ?? `application ${application.id}`} to
                      </label>
                      <select
                        id={`status-${application.id}`}
                        name="status"
                        defaultValue={application.status}
                      >
                        {APPLICATION_STATUSES.map((value) => (
                          <option key={value} value={value}>
                            {statusLabel(value)}
                          </option>
                        ))}
                      </select>
                      <button type="submit" disabled={!writable}>Move</button>
                    </form>
                  </td>
                  <td className="muted">{formatDate(application.appliedAt)}</td>
                  <td className="muted">
                    <OrUnknown value={application.resumeVersion} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={application.interviewStage} />
                  </td>
                  <td className="muted">{formatDate(application.followUpAt)}</td>
                  <td className="muted">{formatDateTime(application.updatedAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}
