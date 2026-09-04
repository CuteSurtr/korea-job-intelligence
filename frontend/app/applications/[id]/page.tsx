import Link from "next/link";
import { notFound } from "next/navigation";
import { BackendError } from "../../components/BackendError";
import { Flash, param } from "../../components/Flash";
import { OrUnknown, StatusBadge } from "../../components/Badges";
import { saveApplication } from "../../lib/actions";
import { APPLICATION_STATUSES, statusLabel } from "../../lib/applications";
import { fetchJson, isNotFound } from "../../lib/api";
import { formatDate, formatDateTime } from "../../lib/format";
import type { Application } from "../../lib/types";

export const dynamic = "force-dynamic";

/** An instant rendered for a date input, which only understands YYYY-MM-DD. */
function dateValue(value: string | null): string {
  if (!value) {
    return "";
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? "" : parsed.toISOString().slice(0, 10);
}

export default async function ApplicationDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { id } = await params;
  const query = await searchParams;

  let application: Application;
  try {
    application = await fetchJson<Application>(`/api/applications/${id}`);
  } catch (error) {
    if (isNotFound(error)) {
      notFound();
    }
    return (
      <>
        <h1>Application {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>
        <OrUnknown value={application.jobTitle} />
      </h1>
      <p className="page-subtitle">
        <OrUnknown value={application.companyName} /> - <StatusBadge status={application.status} />{" "}
        - <Link href={`/jobs/${application.jobId}`}>the posting</Link> -{" "}
        <Link href="/applications">all applications</Link>
      </p>

      <Flash saved={param(query, "saved")} error={param(query, "error")} />

      <form action={saveApplication} className="record-form">
        <input type="hidden" name="id" value={application.id} />

        <fieldset>
          <legend>Status</legend>
          <div className="field-grid">
            <label>
              Status
              <select name="status" defaultValue={application.status}>
                {APPLICATION_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {statusLabel(status)}
                  </option>
                ))}
              </select>
            </label>
            <label className="wide">
              Why it moved
              <input
                name="note"
                placeholder="recorded against the change, only when the status differs"
              />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Application</legend>
          <div className="field-grid">
            <label>
              Applied on
              <input type="date" name="appliedAt" defaultValue={dateValue(application.appliedAt)} />
            </label>
            <label>
              Follow up on
              <input
                type="date"
                name="followUpAt"
                defaultValue={dateValue(application.followUpAt)}
              />
            </label>
            <label>
              Resume version
              <input name="resumeVersion" defaultValue={application.resumeVersion ?? ""} />
            </label>
            <label>
              Cover letter version
              <input
                name="coverLetterVersion"
                defaultValue={application.coverLetterVersion ?? ""}
              />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>People</legend>
          <div className="field-grid">
            <label>
              Contact name
              <input name="contactName" defaultValue={application.contactName ?? ""} />
            </label>
            <label>
              Contact email
              <input
                type="email"
                name="contactEmail"
                defaultValue={application.contactEmail ?? ""}
              />
            </label>
            <label>
              Referral
              <input name="referral" defaultValue={application.referral ?? ""} />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Interview</legend>
          <div className="field-grid">
            <label>
              Stage
              <input name="interviewStage" defaultValue={application.interviewStage ?? ""} />
            </label>
            <label className="wide">
              Interview notes
              <textarea
                name="interviewNotes"
                rows={3}
                defaultValue={application.interviewNotes ?? ""}
              />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Notes</legend>
          <div className="field-grid">
            <label className="wide">
              Notes
              <textarea name="notes" rows={4} defaultValue={application.notes ?? ""} />
            </label>
          </div>
        </fieldset>

        <div className="form-actions">
          <button type="submit">Save</button>
          <Link className="badge reset" href={`/applications/${application.id}`}>
            Discard
          </Link>
        </div>
      </form>

      <h2>Status history</h2>
      <p className="muted">
        Every change is recorded with the status it came from, so this cannot be rewritten by
        editing the form above.
      </p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>From</th>
              <th>To</th>
              <th>Note</th>
            </tr>
          </thead>
          <tbody>
            {application.history.length === 0 ? (
              <tr>
                <td colSpan={4} className="muted">
                  No status change has been recorded.
                </td>
              </tr>
            ) : (
              application.history.map((change, index) => (
                <tr key={`${change.changedAt}-${index}`}>
                  <td className="muted">{formatDateTime(change.changedAt)}</td>
                  <td className="muted">
                    {change.fromStatus ? <StatusBadge status={change.fromStatus} /> : "-"}
                  </td>
                  <td>
                    <StatusBadge status={change.toStatus} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={change.note} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="detail-grid">
        <div>
          <div className="label">Created</div>
          <div>{formatDateTime(application.createdAt)}</div>
        </div>
        <div>
          <div className="label">Updated</div>
          <div>{formatDateTime(application.updatedAt)}</div>
        </div>
        <div>
          <div className="label">Rejected</div>
          <div>{formatDate(application.rejectionAt)}</div>
        </div>
        <div>
          <div className="label">Offer</div>
          <div>{formatDate(application.offerAt)}</div>
        </div>
        <div>
          <div className="label">Profile</div>
          <div>
            <OrUnknown value={application.profileCode} />
          </div>
        </div>
      </div>
    </>
  );
}
