import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { LifecycleBadge, OrUnknown, ScoreCell, SeniorityBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import { formatDeadline, formatExperience, formatRelativeDays } from "../lib/format";
import { SECTORS, sectorLabel } from "../lib/applications";
import type { Job, PageResponse } from "../lib/types";

export const dynamic = "force-dynamic";

const SORTS = [
  ["BEST_MATCH", "Best match"],
  ["HIGHEST_CAREER_VALUE", "Highest SWE value"],
  ["JUNIOR_FRIENDLY", "Junior friendly"],
  ["NEWEST", "Newest"],
  ["CLOSING_SOON", "Closing soon"],
  ["RECENTLY_VERIFIED", "Recently verified"],
  ["MOST_SOURCES", "Most corroborated"],
  ["COMPANY", "Company"],
];

const ROLE_FAMILIES = [
  "BACKEND",
  "PLATFORM",
  "DEVOPS",
  "DATA_INFRASTRUCTURE",
  "FULLSTACK",
  "FRONTEND",
  "MOBILE",
  "MACHINE_LEARNING",
  "SECURITY",
  "EMBEDDED",
  "QA",
];

type SearchParams = Record<string, string | string[] | undefined>;

function first(params: SearchParams, key: string): string {
  const value = params[key];
  return Array.isArray(value) ? (value[0] ?? "") : (value ?? "");
}

export default async function JobsPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;
  const query = {
    keyword: first(params, "keyword"),
    company: first(params, "company"),
    roleFamily: first(params, "roleFamily"),
    sector: first(params, "sector"),
    financialOnly: first(params, "financialOnly") === "true" ? true : undefined,
    excludeLarge: first(params, "excludeLarge") === "true" ? true : undefined,
    seniority: first(params, "seniority"),
    location: first(params, "location"),
    source: first(params, "source"),
    maxYearsExperience: first(params, "maxYearsExperience"),
    minCareerValue: first(params, "minCareerValue"),
    sort: first(params, "sort") || "BEST_MATCH",
    openOnly: first(params, "openOnly") !== "false",
    page: Number(first(params, "page") || "0"),
    size: 50,
  };

  let jobs: PageResponse<Job>;
  try {
    jobs = await fetchJson<PageResponse<Job>>("/api/jobs", query);
  } catch (error) {
    return (
      <>
        <h1>Jobs</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>Jobs</h1>
      <p className="page-subtitle">
        {jobs.totalElements} canonical postings, page {jobs.page + 1} of{" "}
        {Math.max(1, jobs.totalPages)}
      </p>

      <form className="filters" method="get">
        <label>
          Keyword
          <input name="keyword" defaultValue={query.keyword} placeholder="backend" />
        </label>
        <label>
          Company
          <input name="company" defaultValue={query.company} placeholder="Coupang" />
        </label>
        <label>
          Sector
          <select name="sector" defaultValue={query.sector}>
            <option value="">any</option>
            {SECTORS.map((value) => (
              <option key={value} value={value}>
                {sectorLabel(value)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Finance only
          <select name="financialOnly" defaultValue={query.financialOnly ? "true" : ""}>
            <option value="">no</option>
            <option value="true">yes</option>
          </select>
        </label>
        <label>
          Hide big names
          <select name="excludeLarge" defaultValue={query.excludeLarge ? "true" : ""}>
            <option value="">no</option>
            <option value="true">yes</option>
          </select>
        </label>
        <label>
          Role family
          <select name="roleFamily" defaultValue={query.roleFamily}>
            <option value="">any</option>
            {ROLE_FAMILIES.map((family) => (
              <option key={family} value={family}>
                {family.toLowerCase().replace(/_/g, " ")}
              </option>
            ))}
          </select>
        </label>
        <label>
          Seniority
          <select name="seniority" defaultValue={query.seniority}>
            <option value="">any</option>
            <option value="A">A entry</option>
            <option value="A,B">A and B</option>
            <option value="B">B junior</option>
            <option value="C">C stretch</option>
            <option value="D">D experienced</option>
            <option value="X">X senior</option>
          </select>
        </label>
        <label>
          Max experience
          <input
            name="maxYearsExperience"
            defaultValue={query.maxYearsExperience}
            placeholder="2"
            inputMode="numeric"
          />
        </label>
        <label>
          Min SWE value
          <input
            name="minCareerValue"
            defaultValue={query.minCareerValue}
            placeholder="40"
            inputMode="numeric"
          />
        </label>
        <label>
          Location
          <input name="location" defaultValue={query.location} placeholder="Seoul" />
        </label>
        <label>
          Source
          <input name="source" defaultValue={query.source} placeholder="greenhouse" />
        </label>
        <label>
          Sort
          <select name="sort" defaultValue={query.sort}>
            {SORTS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Include closed
          <select name="openOnly" defaultValue={query.openOnly ? "true" : "false"}>
            <option value="true">no</option>
            <option value="false">yes</option>
          </select>
        </label>
        <button type="submit">Apply</button>
        <Link className="badge reset" href="/jobs">
          Reset
        </Link>
      </form>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Company</th>
              <th>Role</th>
              <th>Family</th>
              <th>Sector</th>
              <th>Employer</th>
              <th>Sen.</th>
              <th className="numeric">SWE</th>
              <th className="numeric">Fit</th>
              <th>Location</th>
              <th>Experience</th>
              <th className="numeric">Sources</th>
              <th>Verified</th>
              <th>Deadline</th>
              <th>State</th>
            </tr>
          </thead>
          <tbody>
            {jobs.content.length === 0 ? (
              <tr>
                <td colSpan={12} className="muted">
                  No job matches these filters.
                </td>
              </tr>
            ) : (
              jobs.content.map((job) => (
                <tr key={job.id}>
                  <td>
                    <Link href={`/companies/${job.companyId}`}>{job.companyName}</Link>
                  </td>
                  <td>
                    <Link href={`/jobs/${job.id}`}>{job.title}</Link>
                  </td>
                  <td className="muted">
                    <OrUnknown value={job.roleFamily?.toLowerCase().replace(/_/g, " ")} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={job.sector ? sectorLabel(job.sector) : null} />
                  </td>
                  <td className="muted">
                    {job.companyStage === "LARGE" ? "big name" : job.companyStage === "EMERGING"
                      ? "own board"
                      : "-"}
                  </td>
                  <td>
                    <SeniorityBadge bucket={job.seniorityBucket} />
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
                  <td className="muted">
                    {formatExperience(job.yearsExperienceMin, job.yearsExperienceMax)}
                  </td>
                  <td className="numeric">{job.sourceCount}</td>
                  <td className="muted">{formatRelativeDays(job.lastVerifiedAt)}</td>
                  <td className="muted">
                    {formatDeadline(job.deadlineAt, job.deadlineOpenEnded)}
                  </td>
                  <td>
                    <LifecycleBadge state={job.lifecycleState} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={jobs.page} totalPages={jobs.totalPages} params={params} />
    </>
  );
}

function Pagination({
  page,
  totalPages,
  params,
}: {
  page: number;
  totalPages: number;
  params: SearchParams;
}) {
  const build = (target: number) => {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (key === "page" || value === undefined) {
        continue;
      }
      search.set(key, Array.isArray(value) ? (value[0] ?? "") : value);
    }
    search.set("page", String(target));
    return `/jobs?${search.toString()}`;
  };

  return (
    <div className="pagination">
      {page > 0 ? <Link href={build(page - 1)}>Previous</Link> : <span>Previous</span>}
      <span>
        page {page + 1} of {Math.max(1, totalPages)}
      </span>
      {page + 1 < totalPages ? <Link href={build(page + 1)}>Next</Link> : <span>Next</span>}
    </div>
  );
}
