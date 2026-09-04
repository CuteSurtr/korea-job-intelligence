import Link from "next/link";
import { notFound } from "next/navigation";
import { BackendError } from "../../components/BackendError";
import { Flash, WriteDisabled, param } from "../../components/Flash";
import { StatusBadge } from "../../components/Badges";
import { saveApplication } from "../../lib/actions";
import { APPLICATION_STATUSES, statusLabel } from "../../lib/applications";
import { canWrite, fetchJson, isNotFound } from "../../lib/api";
import { formatDate, formatDateTime } from "../../lib/format";
import type { Application } from "../../lib/types";
import {
  Container,
  Fact,
  FactGrid,
  OrUnknown,
  TableWrap,
  Td,
  Th,
  secondaryButtonClass,
} from "../../components/ui";

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
        <h1 className="text-2xl font-semibold tracking-tight">Application {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="detail">
      <h1 className="text-2xl font-semibold tracking-tight">
        <OrUnknown value={application.jobTitle} />
      </h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        <OrUnknown value={application.companyName} /> - <StatusBadge status={application.status} />{" "}
        - <Link href={`/jobs/${application.jobId}`}>the posting</Link> -{" "}
        <Link href="/applications">all applications</Link>
      </p>

      <Flash saved={param(query, "saved")} error={param(query, "error")} />
      <WriteDisabled />

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
          <button type="submit" disabled={!canWrite()}>Save</button>
          <Link className={secondaryButtonClass} href={`/applications/${application.id}`}>
            Discard
          </Link>
        </div>
      </form>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Status history</h2>
      <p className="text-muted-foreground">
        Every change is recorded with the status it came from, so this cannot be rewritten by
        editing the form above.
      </p>
      <TableWrap>
          <thead>
            <tr>
              <Th>When</Th>
              <Th>From</Th>
              <Th>To</Th>
              <Th>Note</Th>
            </tr>
          </thead>
          <tbody>
            {application.history.length === 0 ? (
              <tr>
                <Td colSpan={4} className="text-muted-foreground">
                  No status change has been recorded.
                </Td>
              </tr>
            ) : (
              application.history.map((change, index) => (
                <tr key={`${change.changedAt}-${index}`}>
                  <Td className="text-muted-foreground">{formatDateTime(change.changedAt)}</Td>
                  <Td className="text-muted-foreground">
                    {change.fromStatus ? <StatusBadge status={change.fromStatus} /> : "-"}
                  </Td>
                  <Td>
                    <StatusBadge status={change.toStatus} />
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={change.note} />
                  </Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>

      <FactGrid>
        <Fact label="Created" value={formatDateTime(application.createdAt)} />
        <Fact label="Updated" value={formatDateTime(application.updatedAt)} />
        <Fact label="Rejected" value={formatDate(application.rejectionAt)} />
        <Fact label="Offer" value={formatDate(application.offerAt)} />
        <Fact label="Profile" value={application.profileCode} />
      </FactGrid>
    </Container>
  );
}
