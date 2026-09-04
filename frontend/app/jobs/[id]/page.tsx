import { notFound } from "next/navigation";
import Link from "next/link";
import { BackendError } from "../../components/BackendError";
import { Flash, WriteDisabled, param } from "../../components/Flash";
import {
  LifecycleBadge,
  ScoreCell,
  SeniorityBadge,
  StatusBadge,
} from "../../components/Badges";
import {
  Badge,
  Container,
  Fact,
  FactGrid,
  OrUnknown,
  TableWrap,
  Td,
  Th,
  Unknown,
  secondaryButtonClass,
} from "../../components/ui";
import { trackJob } from "../../lib/actions";
import { canWrite, fetchJson, isNotFound } from "../../lib/api";
import { APPLICATION_STATUSES, statusLabel } from "../../lib/applications";
import {
  formatDate,
  formatDateTime,
  formatDeadline,
  formatExperience,
} from "../../lib/format";
import type { Application, JobDetail, JobScore, PageResponse } from "../../lib/types";

export const dynamic = "force-dynamic";

export default async function JobDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { id } = await params;
  const query = await searchParams;

  let detail: JobDetail;
  try {
    detail = await fetchJson<JobDetail>(`/api/jobs/${id}`);
  } catch (error) {
    if (isNotFound(error)) {
      notFound();
    }
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Job {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  const job = detail.job;
  const intelligence = detail.intelligence;

  // Whether this job is already tracked decides between offering to start and offering to move.
  // A failure here must not take the posting down with it, so it degrades to the untracked form.
  let tracked: Application | null = null;
  try {
    const existing = await fetchJson<PageResponse<Application>>("/api/applications", {
      jobId: job.id,
    });
    tracked = existing.content[0] ?? null;
  } catch {
    tracked = null;
  }

  return (
    <Container width="detail">
      <h1 className="text-2xl font-semibold tracking-tight">{job.title}</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        <Link href={`/companies/${job.companyId}`}>{job.companyName}</Link>
        {" - "}
        <LifecycleBadge state={job.lifecycleState} />
        {job.applyUrl ? (
          <>
            {" - "}
            <a href={job.applyUrl} target="_blank" rel="noreferrer">
              canonical posting
            </a>
          </>
        ) : null}
      </p>

      <Flash saved={param(query, "saved")} error={param(query, "error")} />
      <WriteDisabled />

      <form action={trackJob} className="track-form">
        <input type="hidden" name="jobId" value={job.id} />
        {tracked ? (
          <>
            <span className="track-label">
              Tracked as <StatusBadge status={tracked.status} />
            </span>
            <label className="sr-only" htmlFor="track-status">
              Move this application to
            </label>
            <select id="track-status" name="status" defaultValue={tracked.status}>
              {APPLICATION_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {statusLabel(status)}
                </option>
              ))}
            </select>
            <input name="note" placeholder="why it moved" />
            <button type="submit" disabled={!canWrite()}>Move</button>
            <Link className={secondaryButtonClass} href={`/applications/${tracked.id}`}>
              Open the application
            </Link>
          </>
        ) : (
          <>
            <span className="track-label">Not tracked yet</span>
            <label className="sr-only" htmlFor="track-status">
              Track this job as
            </label>
            <select id="track-status" name="status" defaultValue="INTERESTED">
              {APPLICATION_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {statusLabel(status)}
                </option>
              ))}
            </select>
            <input name="note" placeholder="why it is worth tracking" />
            <button type="submit" disabled={!canWrite()}>Track</button>
          </>
        )}
      </form>

      <FactGrid>
        <Fact label="Role family" value={job.roleFamily?.toLowerCase().replace(/_/g, " ")} />
        <div>
          <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Seniority</div>
          <div>
            <SeniorityBadge bucket={job.seniorityBucket} />{" "}
            <span className="text-muted-foreground">{intelligence?.seniorityLabel ?? ""}</span>
          </div>
        </div>
        <Fact
          label="Experience"
          value={formatExperience(job.yearsExperienceMin, job.yearsExperienceMax)}
        />
        <Fact label="Degree required" value={job.degreeRequired} />
        <Fact label="Employment type" value={job.employmentType} />
        <Fact label="Remote policy" value={job.remotePolicy} />
        <Fact label="Location" value={job.locationRaw ?? job.locationCity} />
        <Fact label="First seen" value={formatDate(job.firstSeenAt)} />
        <Fact label="Last seen" value={formatDate(job.lastSeenAt)} />
        <Fact label="Last verified" value={formatDate(job.lastVerifiedAt)} />
        <Fact label="Posted" value={formatDate(job.postedAt)} />
        <Fact
          label="Deadline"
          value={formatDeadline(job.deadlineAt, job.deadlineOpenEnded)}
        />
        <Fact label="Sources" value={String(job.sourceCount)} />
        {job.closedAt ? <Fact label="Closed" value={`${formatDate(job.closedAt)} (${job.closedReason})`} /> : null}
      </FactGrid>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Scores</h2>
      {detail.scores.length === 0 ? (
        <p className="text-muted-foreground">No score has been computed for this job.</p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>Kind</Th>
                <Th>Profile</Th>
                <Th align="right">Score</Th>
                <Th align="right">Confidence</Th>
                <Th>Version</Th>
                <Th>Why</Th>
              </tr>
            </thead>
            <tbody>
              {detail.scores.map((score: JobScore) => (
                <tr key={`${score.scoreKind}-${score.profileCode ?? "none"}`}>
                  <Td>{score.scoreKind.toLowerCase().replace(/_/g, " ")}</Td>
                  <Td className="text-muted-foreground">{score.profileCode ?? "-"}</Td>
                  <Td align="right">
                    <ScoreCell value={score.score} />
                  </Td>
                  <Td align="right" className="text-muted-foreground">{score.confidence.toFixed(2)}</Td>
                  <Td className="text-muted-foreground">{score.scoreVersion}</Td>
                  <Td className="align-top">
                    <pre className="evidence">{score.explanation}</pre>
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Extracted fields and their evidence</h2>
      {!intelligence || intelligence.fields.length === 0 ? (
        <p className="text-muted-foreground">
          Nothing was extracted with enough evidence to record. Unknown is the honest answer here.
        </p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>Field</Th>
                <Th>Value</Th>
                <Th align="right">Confidence</Th>
                <Th>Method</Th>
                <Th>Evidence</Th>
                <Th align="right">Snapshot</Th>
              </tr>
            </thead>
            <tbody>
              {intelligence.fields.map((field) => (
                <tr key={field.fieldName}>
                  <Td>{field.fieldName}</Td>
                  <Td>
                    <OrUnknown value={field.fieldValue} />
                  </Td>
                  <Td align="right" className="text-muted-foreground">{field.confidence.toFixed(2)}</Td>
                  <Td className="text-muted-foreground">{field.extractionMethod.toLowerCase().replace(/_/g, " ")}</Td>
                  <Td>
                    <span className="evidence">{field.evidenceText ?? "-"}</span>
                  </Td>
                  <Td align="right" className="text-muted-foreground">{field.evidenceSnapshotId ?? "-"}</Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Skills</h2>
      {!intelligence || intelligence.skills.length === 0 ? (
        <p className="text-muted-foreground">No skill was detected in this posting.</p>
      ) : (
        <div className="panel">
          {["REQUIRED", "PREFERRED", "MENTIONED"].map((level) => {
            const skills = intelligence.skills.filter((s) => s.requirementLevel === level);
            if (skills.length === 0) {
              return null;
            }
            return (
              <div key={level}>
                <h3>{level.toLowerCase()}</h3>
                <div className="skill-list">
                  {skills.map((skill) => (
                    <Badge key={`${level}-${skill.skillSlug}`}>{skill.skillSlug}</Badge>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {intelligence && intelligence.requirements.length > 0 ? (
        <>
          <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Requirements as stated</h2>
          <div className="panel">
            <ul>
              {intelligence.requirements.map((line, index) => (
                <li key={index}>{line}</li>
              ))}
            </ul>
          </div>
        </>
      ) : null}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Where it was found</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th>Source</Th>
              <Th>External id</Th>
              <Th>Matched by</Th>
              <Th align="right">Confidence</Th>
              <Th>Active</Th>
              <Th>First seen</Th>
              <Th>Last seen</Th>
              <Th>Link</Th>
            </tr>
          </thead>
          <tbody>
            {detail.sources.map((source) => (
              <tr key={source.id}>
                <Td>{source.sourceCode}</Td>
                <Td className="text-muted-foreground">{source.externalId ?? source.externalKey}</Td>
                <Td className="text-muted-foreground">{source.matchMethod.toLowerCase().replace(/_/g, " ")}</Td>
                <Td align="right" className="text-muted-foreground">{Number(source.matchConfidence).toFixed(2)}</Td>
                <Td>{source.active ? "yes" : "no"}</Td>
                <Td className="text-muted-foreground">{formatDate(source.firstSeenAt)}</Td>
                <Td className="text-muted-foreground">{formatDate(source.lastSeenAt)}</Td>
                <Td>
                  {source.sourceUrl ? (
                    <a href={source.sourceUrl} target="_blank" rel="noreferrer">
                      open
                    </a>
                  ) : (
                    <Unknown />
                  )}
                </Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Verification history</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th>When</Th>
              <Th>Method</Th>
              <Th>Outcome</Th>
              <Th>Detail</Th>
            </tr>
          </thead>
          <tbody>
            {detail.verifications.map((verification) => (
              <tr key={verification.id}>
                <Td className="text-muted-foreground">{formatDateTime(verification.verifiedAt)}</Td>
                <Td>{verification.method.toLowerCase().replace(/_/g, " ")}</Td>
                <Td>{verification.outcome.toLowerCase()}</Td>
                <Td className="text-muted-foreground">{verification.detail ?? "-"}</Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Lifecycle history</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th>When</Th>
              <Th>From</Th>
              <Th>To</Th>
              <Th>Reason</Th>
            </tr>
          </thead>
          <tbody>
            {detail.lifecycle.map((event) => (
              <tr key={event.id}>
                <Td className="text-muted-foreground">{formatDateTime(event.occurredAt)}</Td>
                <Td className="text-muted-foreground">{event.fromState?.toLowerCase() ?? "-"}</Td>
                <Td>{event.toState.toLowerCase()}</Td>
                <Td className="text-muted-foreground">{event.reasonCode.toLowerCase().replace(/_/g, " ")}</Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Raw snapshots</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th align="right">Id</Th>
              <Th>Source</Th>
              <Th>Fetched</Th>
              <Th>Raw company</Th>
              <Th>Raw experience</Th>
              <Th>Raw education</Th>
              <Th>Raw deadline</Th>
            </tr>
          </thead>
          <tbody>
            {detail.snapshots.map((snapshot) => (
              <tr key={snapshot.id}>
                <Td align="right">{snapshot.id}</Td>
                <Td>{snapshot.sourceCode}</Td>
                <Td className="text-muted-foreground">{formatDateTime(snapshot.fetchedAt)}</Td>
                <Td className="text-muted-foreground">{snapshot.rawCompany ?? "-"}</Td>
                <Td className="text-muted-foreground">{snapshot.rawExperience ?? "-"}</Td>
                <Td className="text-muted-foreground">{snapshot.rawEducation ?? "-"}</Td>
                <Td className="text-muted-foreground">{snapshot.rawDeadline ?? "-"}</Td>
              </tr>
            ))}
          </tbody>
        </TableWrap>

      {detail.description ? (
        <>
          <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Description</h2>
          <div className="panel">
            <pre>{detail.description}</pre>
          </div>
        </>
      ) : null}
    </Container>
  );
}

