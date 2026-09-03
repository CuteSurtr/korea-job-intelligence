import Link from "next/link";
import { BackendError } from "../../components/BackendError";
import { LifecycleBadge, OrUnknown, RiskBadge, ScoreCell } from "../../components/Badges";
import { fetchJson } from "../../lib/api";
import { formatDate, formatDateTime } from "../../lib/format";
import type { Company, Job, PageResponse } from "../../lib/types";

export const dynamic = "force-dynamic";

export default async function CompanyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let company: Company;
  let jobs: PageResponse<Job>;
  try {
    company = await fetchJson<Company>(`/api/companies/${id}`);
    jobs = await fetchJson<PageResponse<Job>>("/api/jobs", {
      company: company.canonicalName,
      openOnly: false,
      size: 100,
      sort: "NEWEST",
    });
  } catch (error) {
    return (
      <>
        <h1>Company {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>{company.canonicalName}</h1>
      <p className="page-subtitle">
        <RiskBadge level={company.riskLevel} /> - {company.openJobCount} open postings
      </p>

      <div className="detail-grid">
        <Field label="Normalized name" value={company.normalizedName} />
        <Field label="Website" value={company.websiteDomain} />
        <Field label="Country" value={company.countryCode} />
        <Field label="Industry" value={company.industry} />
        <Field label="Company type" value={company.companyType} />
        <Field label="Founded" value={formatDate(company.foundedOn)} />
        <Field
          label="Employees"
          value={company.employeeCount === null ? null : String(company.employeeCount)}
        />
        <Field label="Risk assessed" value={formatDate(company.riskAssessedAt)} />
      </div>

      <h2>Known names</h2>
      <div className="panel">
        <div className="skill-list">
          {company.aliases.length === 0 ? (
            <span className="muted">Only the canonical name has been observed.</span>
          ) : (
            company.aliases.map((alias) => (
              <span className="badge" key={alias}>
                {alias}
              </span>
            ))
          )}
        </div>
      </div>

      <h2>Provider identifiers</h2>
      {company.identifiers.length === 0 ? (
        <p className="muted">No provider identifier has been observed for this company.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Type</th>
                <th>Value</th>
                <th>Observed</th>
              </tr>
            </thead>
            <tbody>
              {company.identifiers.map((identifier) => (
                <tr key={`${identifier.type}-${identifier.value}`}>
                  <td>{identifier.type}</td>
                  <td className="muted">{identifier.value}</td>
                  <td className="muted">{formatDateTime(identifier.observedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Metrics</h2>
      {company.metrics.length === 0 ? (
        <p className="muted">
          No company metric has been supplied by a source. Nothing is estimated here.
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Metric</th>
                <th className="numeric">Value</th>
                <th>Unit</th>
                <th>Effective</th>
                <th>Observed</th>
                <th>Evidence</th>
              </tr>
            </thead>
            <tbody>
              {company.metrics.map((metric, index) => (
                <tr key={`${metric.key}-${index}`}>
                  <td>{metric.key}</td>
                  <td className="numeric">
                    <OrUnknown value={metric.numericValue ?? metric.textValue} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={metric.unit} />
                  </td>
                  <td className="muted">{formatDate(metric.effectiveDate)}</td>
                  <td className="muted">{formatDate(metric.observedAt)}</td>
                  <td>
                    {metric.evidenceUrl ? (
                      <a href={metric.evidenceUrl} target="_blank" rel="noreferrer">
                        source
                      </a>
                    ) : (
                      <span className="unknown">none</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Risk reasons</h2>
      {company.riskReasons.length === 0 ? (
        <p className="muted">
          Risk is {company.riskLevel.toLowerCase()} because nothing has been assessed yet, not
          because the company was judged safe.
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Assessed</th>
                <th>Level</th>
                <th>Reason</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {company.riskReasons.map((reason, index) => (
                <tr key={`${reason.reasonCode}-${index}`}>
                  <td className="muted">{formatDate(reason.assessedAt)}</td>
                  <td>
                    <RiskBadge level={reason.riskLevel} />
                  </td>
                  <td>{reason.reasonCode.toLowerCase().replace(/_/g, " ")}</td>
                  <td className="muted">{reason.reasonDetail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Postings</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Role</th>
              <th>Family</th>
              <th className="numeric">SWE</th>
              <th className="numeric">Fit</th>
              <th>Location</th>
              <th>First seen</th>
              <th>State</th>
            </tr>
          </thead>
          <tbody>
            {jobs.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="muted">
                  No posting is tracked for this company.
                </td>
              </tr>
            ) : (
              jobs.content.map((job) => (
                <tr key={job.id}>
                  <td>
                    <Link href={`/jobs/${job.id}`}>{job.title}</Link>
                  </td>
                  <td className="muted">
                    <OrUnknown value={job.roleFamily?.toLowerCase().replace(/_/g, " ")} />
                  </td>
                  <td className="numeric">
                    <ScoreCell value={job.careerValueScore} />
                  </td>
                  <td className="numeric">
                    <ScoreCell value={job.candidateFitScore} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={job.locationCity ?? job.locationRaw} />
                  </td>
                  <td className="muted">{formatDate(job.firstSeenAt)}</td>
                  <td>
                    <LifecycleBadge state={job.lifecycleState} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
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
