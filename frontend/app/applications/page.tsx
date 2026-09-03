import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { OrUnknown, StatusBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDate, formatDateTime } from "../lib/format";
import type { Application, PageResponse } from "../lib/types";

export const dynamic = "force-dynamic";

const STATUSES = [
  "NOT_REVIEWED",
  "INTERESTED",
  "READY_TO_APPLY",
  "APPLIED",
  "INTERVIEW",
  "OFFER",
  "REJECTED",
  "WITHDRAWN",
  "IGNORED",
];

export default async function ApplicationsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const statusParam = params.status;
  const status = Array.isArray(statusParam) ? (statusParam[0] ?? "") : (statusParam ?? "");

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

      <form className="filters" method="get">
        <label>
          Status
          <select name="status" defaultValue={status}>
            <option value="">any</option>
            {STATUSES.map((value) => (
              <option key={value} value={value}>
                {value.toLowerCase().replace(/_/g, " ")}
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
                <td colSpan={8} className="muted">
                  No application has been recorded yet.
                </td>
              </tr>
            ) : (
              applications.content.map((application) => (
                <tr key={application.id}>
                  <td className="muted">
                    <OrUnknown value={application.companyName} />
                  </td>
                  <td>
                    <Link href={`/jobs/${application.jobId}`}>
                      <OrUnknown value={application.jobTitle} />
                    </Link>
                  </td>
                  <td>
                    <StatusBadge status={application.status} />
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
