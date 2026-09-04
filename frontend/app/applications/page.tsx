import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { Flash, WriteDisabled, param } from "../components/Flash";
import { StatusBadge } from "../components/Badges";
import { setApplicationStatus } from "../lib/actions";
import { APPLICATION_STATUSES, statusLabel } from "../lib/applications";
import { canWrite, fetchJson } from "../lib/api";
import { formatDate, formatDateTime } from "../lib/format";
import type { Application, PageResponse } from "../lib/types";
import {
  Container,
  OrUnknown,
  TableWrap,
  Td,
  Th,
  buttonClass,
  secondaryButtonClass,
} from "../components/ui";

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
        <h1 className="text-2xl font-semibold tracking-tight">Applications</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="table">
      <h1 className="text-2xl font-semibold tracking-tight">Applications</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        {applications.totalElements} tracked applications. Every status change is recorded with the
        status it came from.
      </p>

      <Flash saved={param(params, "saved")} error={param(params, "error")} />
      <WriteDisabled />

      <form className="mb-5 flex flex-wrap items-end gap-3 rounded-lg border bg-surface p-3" method="get">
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
        <button type="submit" className={buttonClass}>Apply</button>
        <Link className={secondaryButtonClass} href="/applications">
          Reset
        </Link>
      </form>

      <TableWrap>
          <thead>
            <tr>
              <Th>Company</Th>
              <Th>Role</Th>
              <Th>Status</Th>
              <Th>Move to</Th>
              <Th>Applied</Th>
              <Th>Resume</Th>
              <Th>Interview stage</Th>
              <Th>Follow up</Th>
              <Th>Updated</Th>
            </tr>
          </thead>
          <tbody>
            {applications.content.length === 0 ? (
              <tr>
                <Td colSpan={9} className="text-muted-foreground">
                  {status
                    ? `No application is ${statusLabel(status)}.`
                    : "No application has been recorded yet. Open a job and track it."}
                </Td>
              </tr>
            ) : (
              applications.content.map((application) => (
                <tr key={application.id}>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={application.companyName} />
                  </Td>
                  <Td>
                    <Link href={`/applications/${application.id}`}>
                      <OrUnknown value={application.jobTitle} />
                    </Link>
                  </Td>
                  <Td>
                    <StatusBadge status={application.status} />
                  </Td>
                  <Td>
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
                  </Td>
                  <Td className="text-muted-foreground">{formatDate(application.appliedAt)}</Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={application.resumeVersion} />
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={application.interviewStage} />
                  </Td>
                  <Td className="text-muted-foreground">{formatDate(application.followUpAt)}</Td>
                  <Td className="text-muted-foreground">{formatDateTime(application.updatedAt)}</Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>
    </Container>
  );
}
