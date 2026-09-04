import { notFound } from "next/navigation";
import Link from "next/link";
import { BackendError } from "../../components/BackendError";
import {
  LifecycleBadge,
  OrUnknown,
  ScoreCell,
  SeniorityBadge,
  Unknown,
} from "../../components/Badges";
import { fetchJson, isNotFound } from "../../lib/api";
import {
  formatDate,
  formatDateTime,
  formatDeadline,
  formatExperience,
} from "../../lib/format";
import type { JobDetail, JobScore } from "../../lib/types";

export const dynamic = "force-dynamic";

export default async function JobDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let detail: JobDetail;
  try {
    detail = await fetchJson<JobDetail>(`/api/jobs/${id}`);
  } catch (error) {
    if (isNotFound(error)) {
      notFound();
    }
    return (
      <>
        <h1>Job {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  const job = detail.job;
  const intelligence = detail.intelligence;

  return (
    <>
      <h1>{job.title}</h1>
      <p className="page-subtitle">
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

      <div className="detail-grid">
        <Field label="Role family" value={job.roleFamily?.toLowerCase().replace(/_/g, " ")} />
        <div>
          <div className="label">Seniority</div>
          <div>
            <SeniorityBadge bucket={job.seniorityBucket} />{" "}
            <span className="muted">{intelligence?.seniorityLabel ?? ""}</span>
          </div>
        </div>
        <Field
          label="Experience"
          value={formatExperience(job.yearsExperienceMin, job.yearsExperienceMax)}
        />
        <Field label="Degree required" value={job.degreeRequired} />
        <Field label="Employment type" value={job.employmentType} />
        <Field label="Remote policy" value={job.remotePolicy} />
        <Field label="Location" value={job.locationRaw ?? job.locationCity} />
        <Field label="First seen" value={formatDate(job.firstSeenAt)} />
        <Field label="Last seen" value={formatDate(job.lastSeenAt)} />
        <Field label="Last verified" value={formatDate(job.lastVerifiedAt)} />
        <Field label="Posted" value={formatDate(job.postedAt)} />
        <Field
          label="Deadline"
          value={formatDeadline(job.deadlineAt, job.deadlineOpenEnded)}
        />
        <Field label="Sources" value={String(job.sourceCount)} />
        {job.closedAt ? <Field label="Closed" value={`${formatDate(job.closedAt)} (${job.closedReason})`} /> : null}
      </div>

      <h2>Scores</h2>
      {detail.scores.length === 0 ? (
        <p className="muted">No score has been computed for this job.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Kind</th>
                <th>Profile</th>
                <th className="numeric">Score</th>
                <th className="numeric">Confidence</th>
                <th>Version</th>
                <th>Why</th>
              </tr>
            </thead>
            <tbody>
              {detail.scores.map((score: JobScore) => (
                <tr key={`${score.scoreKind}-${score.profileCode ?? "none"}`}>
                  <td>{score.scoreKind.toLowerCase().replace(/_/g, " ")}</td>
                  <td className="muted">{score.profileCode ?? "-"}</td>
                  <td className="numeric">
                    <ScoreCell value={score.score} />
                  </td>
                  <td className="numeric muted">{score.confidence.toFixed(2)}</td>
                  <td className="muted">{score.scoreVersion}</td>
                  <td>
                    <pre className="evidence">{score.explanation}</pre>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Extracted fields and their evidence</h2>
      {!intelligence || intelligence.fields.length === 0 ? (
        <p className="muted">
          Nothing was extracted with enough evidence to record. Unknown is the honest answer here.
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Field</th>
                <th>Value</th>
                <th className="numeric">Confidence</th>
                <th>Method</th>
                <th>Evidence</th>
                <th className="numeric">Snapshot</th>
              </tr>
            </thead>
            <tbody>
              {intelligence.fields.map((field) => (
                <tr key={field.fieldName}>
                  <td>{field.fieldName}</td>
                  <td>
                    <OrUnknown value={field.fieldValue} />
                  </td>
                  <td className="numeric muted">{field.confidence.toFixed(2)}</td>
                  <td className="muted">{field.extractionMethod.toLowerCase().replace(/_/g, " ")}</td>
                  <td>
                    <span className="evidence">{field.evidenceText ?? "-"}</span>
                  </td>
                  <td className="numeric muted">{field.evidenceSnapshotId ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Skills</h2>
      {!intelligence || intelligence.skills.length === 0 ? (
        <p className="muted">No skill was detected in this posting.</p>
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
                    <span className="badge" key={`${level}-${skill.skillSlug}`}>
                      {skill.skillSlug}
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {intelligence && intelligence.requirements.length > 0 ? (
        <>
          <h2>Requirements as stated</h2>
          <div className="panel">
            <ul>
              {intelligence.requirements.map((line, index) => (
                <li key={index}>{line}</li>
              ))}
            </ul>
          </div>
        </>
      ) : null}

      <h2>Where it was found</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>External id</th>
              <th>Matched by</th>
              <th className="numeric">Confidence</th>
              <th>Active</th>
              <th>First seen</th>
              <th>Last seen</th>
              <th>Link</th>
            </tr>
          </thead>
          <tbody>
            {detail.sources.map((source) => (
              <tr key={source.id}>
                <td>{source.sourceCode}</td>
                <td className="muted">{source.externalId ?? source.externalKey}</td>
                <td className="muted">{source.matchMethod.toLowerCase().replace(/_/g, " ")}</td>
                <td className="numeric muted">{Number(source.matchConfidence).toFixed(2)}</td>
                <td>{source.active ? "yes" : "no"}</td>
                <td className="muted">{formatDate(source.firstSeenAt)}</td>
                <td className="muted">{formatDate(source.lastSeenAt)}</td>
                <td>
                  {source.sourceUrl ? (
                    <a href={source.sourceUrl} target="_blank" rel="noreferrer">
                      open
                    </a>
                  ) : (
                    <Unknown />
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Verification history</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>Method</th>
              <th>Outcome</th>
              <th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {detail.verifications.map((verification) => (
              <tr key={verification.id}>
                <td className="muted">{formatDateTime(verification.verifiedAt)}</td>
                <td>{verification.method.toLowerCase().replace(/_/g, " ")}</td>
                <td>{verification.outcome.toLowerCase()}</td>
                <td className="muted">{verification.detail ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Lifecycle history</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>From</th>
              <th>To</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {detail.lifecycle.map((event) => (
              <tr key={event.id}>
                <td className="muted">{formatDateTime(event.occurredAt)}</td>
                <td className="muted">{event.fromState?.toLowerCase() ?? "-"}</td>
                <td>{event.toState.toLowerCase()}</td>
                <td className="muted">{event.reasonCode.toLowerCase().replace(/_/g, " ")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Raw snapshots</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th className="numeric">Id</th>
              <th>Source</th>
              <th>Fetched</th>
              <th>Raw company</th>
              <th>Raw experience</th>
              <th>Raw education</th>
              <th>Raw deadline</th>
            </tr>
          </thead>
          <tbody>
            {detail.snapshots.map((snapshot) => (
              <tr key={snapshot.id}>
                <td className="numeric">{snapshot.id}</td>
                <td>{snapshot.sourceCode}</td>
                <td className="muted">{formatDateTime(snapshot.fetchedAt)}</td>
                <td className="muted">{snapshot.rawCompany ?? "-"}</td>
                <td className="muted">{snapshot.rawExperience ?? "-"}</td>
                <td className="muted">{snapshot.rawEducation ?? "-"}</td>
                <td className="muted">{snapshot.rawDeadline ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {detail.description ? (
        <>
          <h2>Description</h2>
          <div className="panel">
            <pre>{detail.description}</pre>
          </div>
        </>
      ) : null}
    </>
  );
}

function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <div className="label">{label}</div>
      <div>
        <OrUnknown value={value} />
      </div>
    </div>
  );
}
