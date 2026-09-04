import { notFound } from "next/navigation";
import Link from "next/link";
import { BackendError } from "../../components/BackendError";
import { LifecycleBadge, RiskBadge, ScoreCell } from "../../components/Badges";
import { fetchJson, isNotFound } from "../../lib/api";
import { formatDate, formatDateTime } from "../../lib/format";
import type { Company, Job, PageResponse } from "../../lib/types";
import {
  Badge,
  Container,
  Fact,
  FactGrid,
  OrUnknown,
  TableWrap,
  Td,
  Th,
} from "../../components/ui";

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
    if (isNotFound(error)) {
      notFound();
    }
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Company {id}</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="detail">
      <h1 className="text-2xl font-semibold tracking-tight">{company.canonicalName}</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        <RiskBadge level={company.riskLevel} /> - {company.openJobCount} open postings
      </p>

      <FactGrid>
        <Fact label="Normalized name" value={company.normalizedName} />
        <Fact label="Website" value={company.websiteDomain} />
        <Fact label="Country" value={company.countryCode} />
        <Fact label="Industry" value={company.industry} />
        <Fact label="Company type" value={company.companyType} />
        <Fact label="Founded" value={formatDate(company.foundedOn)} />
        <Fact
          label="Employees"
          value={company.employeeCount === null ? null : String(company.employeeCount)}
        />
        <Fact label="Risk assessed" value={formatDate(company.riskAssessedAt)} />
      </FactGrid>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Known names</h2>
      <div className="panel">
        <div className="skill-list">
          {company.aliases.length === 0 ? (
            <span className="text-muted-foreground">Only the canonical name has been observed.</span>
          ) : (
            company.aliases.map((alias) => (
              <Badge key={alias}>{alias}</Badge>
            ))
          )}
        </div>
      </div>

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Provider identifiers</h2>
      {company.identifiers.length === 0 ? (
        <p className="text-muted-foreground">No provider identifier has been observed for this company.</p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>Type</Th>
                <Th>Value</Th>
                <Th>Observed</Th>
              </tr>
            </thead>
            <tbody>
              {company.identifiers.map((identifier) => (
                <tr key={`${identifier.type}-${identifier.value}`}>
                  <Td>{identifier.type}</Td>
                  <Td className="text-muted-foreground">{identifier.value}</Td>
                  <Td className="text-muted-foreground">{formatDateTime(identifier.observedAt)}</Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Metrics</h2>
      {company.metrics.length === 0 ? (
        <p className="text-muted-foreground">
          No company metric has been supplied by a source. Nothing is estimated here.
        </p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>Metric</Th>
                <Th align="right">Value</Th>
                <Th>Unit</Th>
                <Th>Effective</Th>
                <Th>Observed</Th>
                <Th>Evidence</Th>
              </tr>
            </thead>
            <tbody>
              {company.metrics.map((metric, index) => (
                <tr key={`${metric.key}-${index}`}>
                  <Td>{metric.key}</Td>
                  <Td align="right">
                    <OrUnknown value={metric.numericValue ?? metric.textValue} />
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={metric.unit} />
                  </Td>
                  <Td className="text-muted-foreground">{formatDate(metric.effectiveDate)}</Td>
                  <Td className="text-muted-foreground">{formatDate(metric.observedAt)}</Td>
                  <Td>
                    {metric.evidenceUrl ? (
                      <a href={metric.evidenceUrl} target="_blank" rel="noreferrer">
                        source
                      </a>
                    ) : (
                      <span className="unknown">none</span>
                    )}
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Risk reasons</h2>
      {company.riskReasons.length === 0 ? (
        <p className="text-muted-foreground">
          Risk is {company.riskLevel.toLowerCase()} because nothing has been assessed yet, not
          because the company was judged safe.
        </p>
      ) : (
        <TableWrap>
            <thead>
              <tr>
                <Th>Assessed</Th>
                <Th>Level</Th>
                <Th>Reason</Th>
                <Th>Detail</Th>
              </tr>
            </thead>
            <tbody>
              {company.riskReasons.map((reason, index) => (
                <tr key={`${reason.reasonCode}-${index}`}>
                  <Td className="text-muted-foreground">{formatDate(reason.assessedAt)}</Td>
                  <Td>
                    <RiskBadge level={reason.riskLevel} />
                  </Td>
                  <Td>{reason.reasonCode.toLowerCase().replace(/_/g, " ")}</Td>
                  <Td className="text-muted-foreground">{reason.reasonDetail}</Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
      )}

      <h2 className="mt-6 mb-3 text-sm font-semibold tracking-tight">Postings</h2>
      <TableWrap>
          <thead>
            <tr>
              <Th>Role</Th>
              <Th>Family</Th>
              <Th align="right">SWE</Th>
              <Th align="right">Fit</Th>
              <Th>Location</Th>
              <Th>First seen</Th>
              <Th>State</Th>
            </tr>
          </thead>
          <tbody>
            {jobs.content.length === 0 ? (
              <tr>
                <Td colSpan={7} className="text-muted-foreground">
                  No posting is tracked for this company.
                </Td>
              </tr>
            ) : (
              jobs.content.map((job) => (
                <tr key={job.id}>
                  <Td>
                    <Link href={`/jobs/${job.id}`}>{job.title}</Link>
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={job.roleFamily?.toLowerCase().replace(/_/g, " ")} />
                  </Td>
                  <Td align="right">
                    <ScoreCell value={job.careerValueScore} />
                  </Td>
                  <Td align="right">
                    <ScoreCell value={job.candidateFitScore} />
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={job.locationCity ?? job.locationRaw} />
                  </Td>
                  <Td className="text-muted-foreground">{formatDate(job.firstSeenAt)}</Td>
                  <Td>
                    <LifecycleBadge state={job.lifecycleState} />
                  </Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>
    </Container>
  );
}

