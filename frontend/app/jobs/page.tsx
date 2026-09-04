import Link from "next/link";
import { ChevronLeft, ChevronRight, SlidersHorizontal } from "lucide-react";
import { BackendError } from "../components/BackendError";
import { LifecycleBadge, ScoreCell, SeniorityBadge } from "../components/Badges";
import {
  Badge,
  Container,
  Empty,
  Field,
  PageHeader,
  TableWrap,
  Td,
  Th,
  Truncate,
  buttonClass,
  controlClass,
  secondaryButtonClass,
} from "../components/ui";
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
      <Container width="table">
        <PageHeader title="Jobs" />
        <BackendError error={error} />
      </Container>
    );
  }

  // Which of the secondary filters are actually set. The panel below opens by itself when any
  // of them are, so a filtered list never looks unfiltered.
  const advanced = [
    query.roleFamily,
    query.seniority,
    query.location,
    query.source,
    query.maxYearsExperience,
    query.minCareerValue,
  ].filter(Boolean).length;

  return (
    <Container width="table">
      <PageHeader
        title="Jobs"
        subtitle={`${jobs.totalElements} canonical postings, page ${jobs.page + 1} of ${Math.max(
          1,
          jobs.totalPages,
        )}`}
      />

      {/*
        The three questions this console exists to answer sit on one line, always visible. The
        other nine filters live behind a disclosure, because a wall of thirteen boxes makes the
        common case as hard to reach as the rare one.
      */}
      <form method="get" className="mb-5 rounded-lg border bg-surface">
        <div className="flex flex-wrap items-end gap-3 p-3">
          <Field label="Keyword" className="min-w-[13rem] flex-1">
            <input
              name="keyword"
              defaultValue={query.keyword}
              placeholder="backend"
              className={controlClass}
            />
          </Field>
          <Field label="Sector" className="w-44">
            <select name="sector" defaultValue={query.sector} className={controlClass}>
              <option value="">any sector</option>
              {SECTORS.map((value) => (
                <option key={value} value={value}>
                  {sectorLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Finance only" className="w-32">
            <select
              name="financialOnly"
              defaultValue={query.financialOnly ? "true" : ""}
              className={controlClass}
            >
              <option value="">no</option>
              <option value="true">yes</option>
            </select>
          </Field>
          <Field label="Hide big names" className="w-36">
            <select
              name="excludeLarge"
              defaultValue={query.excludeLarge ? "true" : ""}
              className={controlClass}
            >
              <option value="">no</option>
              <option value="true">yes</option>
            </select>
          </Field>
          <Field label="Sort" className="w-48">
            <select name="sort" defaultValue={query.sort} className={controlClass}>
              {SORTS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
          <button type="submit" className={buttonClass}>
            Apply
          </button>
          <Link href="/jobs" className={secondaryButtonClass}>
            Reset
          </Link>
        </div>

        <details open={advanced > 0} className="border-t">
          <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-sm text-muted-foreground hover:text-foreground">
            <SlidersHorizontal className="h-4 w-4" aria-hidden="true" />
            More filters
            {advanced > 0 ? (
              <Badge tone="brand">
                {advanced} active
              </Badge>
            ) : null}
          </summary>
          <div className="grid grid-cols-2 gap-3 border-t p-3 sm:grid-cols-3 lg:grid-cols-6">
            <Field label="Company">
              <input
                name="company"
                defaultValue={query.company}
                placeholder="Coupang"
                className={controlClass}
              />
            </Field>
            <Field label="Role family">
              <select name="roleFamily" defaultValue={query.roleFamily} className={controlClass}>
                <option value="">any</option>
                {ROLE_FAMILIES.map((family) => (
                  <option key={family} value={family}>
                    {family.toLowerCase().replace(/_/g, " ")}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Seniority">
              <select name="seniority" defaultValue={query.seniority} className={controlClass}>
                <option value="">any</option>
                <option value="A">A entry</option>
                <option value="A,B">A and B</option>
                <option value="B">B junior</option>
                <option value="C">C stretch</option>
                <option value="D">D experienced</option>
                <option value="X">X senior</option>
              </select>
            </Field>
            <Field label="Max experience">
              <input
                name="maxYearsExperience"
                defaultValue={query.maxYearsExperience}
                placeholder="2"
                inputMode="numeric"
                className={controlClass}
              />
            </Field>
            <Field label="Min SWE value">
              <input
                name="minCareerValue"
                defaultValue={query.minCareerValue}
                placeholder="40"
                inputMode="numeric"
                className={controlClass}
              />
            </Field>
            <Field label="Location">
              <input
                name="location"
                defaultValue={query.location}
                placeholder="Seoul"
                className={controlClass}
              />
            </Field>
            <Field label="Source">
              <input
                name="source"
                defaultValue={query.source}
                placeholder="greenhouse"
                className={controlClass}
              />
            </Field>
            <Field label="Include closed">
              <select
                name="openOnly"
                defaultValue={query.openOnly ? "true" : "false"}
                className={controlClass}
              >
                <option value="true">no</option>
                <option value="false">yes</option>
              </select>
            </Field>
          </div>
        </details>
      </form>

      <TableWrap>
        <thead>
          <tr>
            <Th>Role</Th>
            <Th>Company</Th>
            <Th>Sen.</Th>
            <Th align="right">SWE</Th>
            <Th align="right">Fit</Th>
            <Th>Location</Th>
            <Th>Experience</Th>
            <Th>Verified</Th>
            <Th>Deadline</Th>
            <Th>State</Th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {jobs.content.length === 0 ? (
            <tr>
              <td colSpan={10}>
                <Empty>No job matches these filters.</Empty>
              </td>
            </tr>
          ) : (
            jobs.content.map((job) => (
              <tr key={job.id} className="hover:bg-muted/40">
                {/*
                  Role carries its own classification underneath rather than in two more
                  columns, and company carries the employer badge. Fourteen columns did not fit
                  on a 1600px screen; stacking the secondary value under the primary is how the
                  same information fits in ten.
                */}
                <Td>
                  <Link href={`/jobs/${job.id}`} className="font-medium hover:underline">
                    <Truncate value={job.title} width="26rem" />
                  </Link>
                  <div className="mt-0.5 text-xs text-muted-foreground">
                    {[
                      job.roleFamily?.toLowerCase().replace(/_/g, " "),
                      job.sector ? sectorLabel(job.sector) : null,
                    ]
                      .filter(Boolean)
                      .join(" · ") || "unclassified"}
                    {job.sourceCount > 1 ? ` · ${job.sourceCount} sources` : ""}
                  </div>
                </Td>
                <Td>
                  <Link
                    href={`/companies/${job.companyId}`}
                    className="text-brand hover:underline"
                  >
                    <Truncate value={job.companyName} width="12rem" />
                  </Link>
                  <div className="mt-0.5 text-xs">
                    {job.companyStage === "LARGE" ? (
                      <span className="text-muted-foreground">big name</span>
                    ) : job.companyStage === "EMERGING" ? (
                      <span className="text-brand">own board</span>
                    ) : (
                      // Nothing to say about this employer's stage. The line still occupies its
                      // height so every row stays exactly two lines tall — a column of dashes
                      // would be furniture standing in for information that does not exist.
                      <span className="invisible">—</span>
                    )}
                  </div>
                </Td>
                <Td>
                  <SeniorityBadge bucket={job.seniorityBucket} />
                </Td>
                <Td align="right">
                  <ScoreCell value={job.careerValueScore} />
                </Td>
                <Td align="right">
                  <ScoreCell value={job.candidateFitScore} />
                </Td>
                <Td className="text-muted-foreground">
                  {/* Some sources put a full street address in this field, so it is capped. */}
                  <Truncate value={job.locationCity ?? job.locationRaw} width="10rem" />
                </Td>
                <Td className="text-muted-foreground">
                  {formatExperience(job.yearsExperienceMin, job.yearsExperienceMax)}
                </Td>
                <Td className="text-muted-foreground">
                  {formatRelativeDays(job.lastVerifiedAt)}
                </Td>
                <Td className="text-muted-foreground">
                  {formatDeadline(job.deadlineAt, job.deadlineOpenEnded)}
                </Td>
                <Td>
                  <LifecycleBadge state={job.lifecycleState} />
                </Td>
              </tr>
            ))
          )}
        </tbody>
      </TableWrap>

      <Pagination page={jobs.page} totalPages={jobs.totalPages} params={params} />
    </Container>
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

  const disabled = "inline-flex h-9 items-center gap-1 rounded-md px-3 text-sm text-muted-foreground/50";

  return (
    <div className="mt-4 flex items-center justify-between gap-3">
      {page > 0 ? (
        <Link href={build(page - 1)} className={secondaryButtonClass}>
          <ChevronLeft className="h-4 w-4" aria-hidden="true" />
          Previous
        </Link>
      ) : (
        <span className={disabled}>
          <ChevronLeft className="h-4 w-4" aria-hidden="true" />
          Previous
        </span>
      )}
      <span className="text-sm text-muted-foreground">
        page {page + 1} of {Math.max(1, totalPages)}
      </span>
      {page + 1 < totalPages ? (
        <Link href={build(page + 1)} className={secondaryButtonClass}>
          Next
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </Link>
      ) : (
        <span className={disabled}>
          Next
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </span>
      )}
    </div>
  );
}
