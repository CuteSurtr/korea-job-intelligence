import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import JobsPage from "../app/jobs/page";
import JobDetailPage from "../app/jobs/[id]/page";
import CompaniesPage from "../app/companies/page";
import CompanyDetailPage from "../app/companies/[id]/page";
import DashboardPage from "../app/page";
import SourcesPage from "../app/sources/page";
import SearchRunsPage from "../app/search-runs/page";
import SearchRunDetailPage from "../app/search-runs/[id]/page";
import ApplicationsPage from "../app/applications/page";

import {
  fixtures,
  renderPage,
  routeParams,
  searchParams,
  stubBackend,
  stubUnreachableBackend,
} from "./support";

const firstJob = fixtures.jobsPage.content[0];
const detailJob = fixtures.jobDetail.job;

describe("jobs list", () => {
  it("renders a row per posting with its company and lifecycle", async () => {
    stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(JobsPage({ searchParams: searchParams() }));

    expect(screen.getByRole("heading", { level: 1, name: "Jobs" })).toBeInTheDocument();
    expect(
      screen.getByText(`${fixtures.jobsPage.totalElements} canonical postings`, { exact: false }),
    ).toBeInTheDocument();

    const rows = screen.getAllByRole("row").slice(1);
    expect(rows).toHaveLength(fixtures.jobsPage.content.length);
    expect(
      within(rows[0]).getByRole("link", { name: firstJob.title }),
    ).toHaveAttribute("href", `/jobs/${firstJob.id}`);
  });

  it("passes the filters and sort from the URL through to the API", async () => {
    const fetchMock = stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(
      JobsPage({
        searchParams: searchParams({
          keyword: "backend",
          roleFamily: "BACKEND",
          maxYearsExperience: "3",
          sort: "JUNIOR_FRIENDLY",
          openOnly: "false",
          page: "2",
        }),
      }),
    );

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.get("keyword")).toBe("backend");
    expect(url.searchParams.get("roleFamily")).toBe("BACKEND");
    expect(url.searchParams.get("maxYearsExperience")).toBe("3");
    expect(url.searchParams.get("sort")).toBe("JUNIOR_FRIENDLY");
    expect(url.searchParams.get("openOnly")).toBe("false");
    expect(url.searchParams.get("page")).toBe("2");
  });

  it("defaults to best match over open postings only", async () => {
    const fetchMock = stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(JobsPage({ searchParams: searchParams() }));

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.get("sort")).toBe("BEST_MATCH");
    expect(url.searchParams.get("openOnly")).toBe("true");
  });

  it("passes the sector filter through to the API", async () => {
    const fetchMock = stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(JobsPage({ searchParams: searchParams({ sector: "BANKING" }) }));

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.get("sector")).toBe("BANKING");
  });

  it("asks for financial roles without naming the sectors", async () => {
    // "financial engineering roles" is the question; which of the five financial sectors a
    // posting landed in is the API's business, not the searcher's.
    const fetchMock = stubBackend({ "/api/jobs": fixtures.financialJobsPage });
    await renderPage(JobsPage({ searchParams: searchParams({ financialOnly: "true" }) }));

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.get("financialOnly")).toBe("true");
    expect(url.searchParams.has("sector")).toBe(false);
  });

  it("does not ask for financial roles unless it was asked to", async () => {
    const fetchMock = stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(JobsPage({ searchParams: searchParams() }));

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.has("financialOnly")).toBe(false);
  });

  it("shows each posting's sector, and says nothing where none was established", async () => {
    stubBackend({ "/api/jobs": fixtures.jobsPage });
    await renderPage(JobsPage({ searchParams: searchParams() }));

    const rows = screen.getAllByRole("row").slice(1);
    for (const [index, job] of fixtures.jobsPage.content.entries()) {
      const cells = within(rows[index]).getAllByRole("cell");
      const text = cells.map((c) => c.textContent).join(" | ");
      if (job.sector) {
        expect(text).toContain(job.sector.toLowerCase().replace(/_/g, " "));
      } else {
        expect(text).toContain("unknown");
      }
    }
  });

  it("keeps the filter form usable when nothing matched", async () => {
    stubBackend({ "/api/jobs": fixtures.emptyPage });
    await renderPage(JobsPage({ searchParams: searchParams({ keyword: "nothing" }) }));

    expect(screen.getByText("0 canonical postings", { exact: false })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Apply" })).toBeInTheDocument();
  });

  it("says the backend is unreachable rather than rendering an empty table", async () => {
    stubUnreachableBackend();
    await renderPage(JobsPage({ searchParams: searchParams() }));

    expect(screen.getByText("The console could not reach the API.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("distinguishes an API error from an outage", async () => {
    stubBackend({ "/api/jobs": { body: { error: "server_error" }, status: 500 } });
    await renderPage(JobsPage({ searchParams: searchParams() }));

    expect(screen.getByText("The API rejected this request.")).toBeInTheDocument();
    expect(screen.queryByText("The console could not reach the API.")).not.toBeInTheDocument();
  });
});

describe("job detail", () => {
  it("shows the posting with its evidence chain", async () => {
    stubBackend({
      "/api/jobs": fixtures.jobDetail,
      "/api/applications": fixtures.untrackedForJob,
    });
    await renderPage(
      JobDetailPage({
        params: routeParams({ id: String(detailJob.id) }),
        searchParams: searchParams(),
      }),
    );

    expect(screen.getByRole("heading", { level: 1, name: detailJob.title })).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: detailJob.companyName }),
    ).toHaveAttribute("href", `/companies/${detailJob.companyId}`);
  });

  it("raises a not-found instead of claiming the backend is down", async () => {
    stubBackend({
      "/api/jobs": { body: { error: "not_found", message: "No job with id 999999" }, status: 404 },
    });

    // notFound() signals App Router to render the 404 segment; it must not be swallowed into
    // the "could not reach the API" panel, which sends people to restart a healthy stack.
    await expect(
      JobDetailPage({ params: routeParams({ id: "999999" }), searchParams: searchParams() }),
    ).rejects.toThrow(/NEXT_HTTP_ERROR_FALLBACK|NEXT_NOT_FOUND/);
  });

  it("still reports a genuine outage as an outage", async () => {
    stubUnreachableBackend();
    await renderPage(
      JobDetailPage({ params: routeParams({ id: "1" }), searchParams: searchParams() }),
    );
    expect(screen.getByText("The console could not reach the API.")).toBeInTheDocument();
  });
});

describe("companies", () => {
  it("lists companies", async () => {
    stubBackend({ "/api/companies": fixtures.companies });
    await renderPage(CompaniesPage());

    expect(screen.getByRole("heading", { level: 1, name: "Companies" })).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: fixtures.companies[0].canonicalName }),
    ).toHaveAttribute("href", `/companies/${fixtures.companies[0].id}`);
  });

  it("shows a company beside its postings", async () => {
    stubBackend({
      "/api/companies": fixtures.company,
      "/api/jobs": fixtures.companyJobs,
    });
    await renderPage(
      CompanyDetailPage({ params: routeParams({ id: String(fixtures.company.id) }) }),
    );

    expect(
      screen.getByRole("heading", { level: 1, name: fixtures.company.canonicalName }),
    ).toBeInTheDocument();
    expect(screen.getByText(firstJob.title)).toBeInTheDocument();
  });

  it("raises a not-found for a company that does not exist", async () => {
    stubBackend({ "/api/companies": { body: { error: "not_found" }, status: 404 } });
    await expect(
      CompanyDetailPage({ params: routeParams({ id: "999999" }) }),
    ).rejects.toThrow(/NEXT_HTTP_ERROR_FALLBACK|NEXT_NOT_FOUND/);
  });
});

describe("dashboard", () => {
  it("reports the counters the pipeline produced", async () => {
    stubBackend({ "/api/dashboard": fixtures.dashboard });
    const { container } = await renderPage(DashboardPage());

    const stat = (label: string) => {
      const node = within(container).getByText(label).closest("div")?.parentElement;
      return node?.textContent ?? "";
    };
    expect(stat("Open jobs")).toContain(String(fixtures.dashboard.totalJobs));
    expect(stat("Junior accessible")).toContain(
      String(fixtures.dashboard.juniorAccessibleOpenJobs),
    );
    expect(stat("Healthy sources")).toContain(String(fixtures.dashboard.healthySources));
  });

  it("explains an unreachable backend on the landing page", async () => {
    stubUnreachableBackend();
    await renderPage(DashboardPage());
    expect(screen.getByText("The console could not reach the API.")).toBeInTheDocument();
  });
});

describe("sources", () => {
  it("lists the registry beside its runtime health", async () => {
    stubBackend({
      "/api/sources": fixtures.sources,
      "/api/sources/health": fixtures.sourceHealth,
    });
    await renderPage(SourcesPage());

    expect(screen.getByRole("heading", { level: 1, name: "Sources" })).toBeInTheDocument();
    for (const source of fixtures.sources) {
      expect(screen.getByText(source.displayName)).toBeInTheDocument();
    }
  });

  it("reports an outage without rendering a half-filled registry", async () => {
    stubUnreachableBackend();
    await renderPage(SourcesPage());
    expect(screen.getByText("The console could not reach the API.")).toBeInTheDocument();
  });
});

describe("search runs", () => {
  it("lists runs and links to each one", async () => {
    stubBackend({ "/api/search-runs": fixtures.searchRuns });
    await renderPage(SearchRunsPage({ searchParams: searchParams() }));

    for (const run of fixtures.searchRuns.content) {
      const link = screen
        .getAllByRole("link")
        .find((node) => node.getAttribute("href") === `/search-runs/${run.id}`);
      expect(link, `no link to run ${run.id}`).toBeDefined();
    }
  });

  it("shows one run with its counters", async () => {
    stubBackend({ "/api/search-runs": fixtures.searchRun });
    await renderPage(
      SearchRunDetailPage({ params: routeParams({ id: String(fixtures.searchRun.id) }) }),
    );

    expect(screen.getByText(fixtures.searchRun.runUuid)).toBeInTheDocument();
    const received = screen.getByText("Records received").closest("div")?.parentElement;
    expect(received?.textContent).toContain(String(fixtures.searchRun.recordsReceived));
  });

  it("raises a not-found for a run that does not exist", async () => {
    stubBackend({ "/api/search-runs": { body: { error: "not_found" }, status: 404 } });
    await expect(
      SearchRunDetailPage({ params: routeParams({ id: "999999" }) }),
    ).rejects.toThrow(/NEXT_HTTP_ERROR_FALLBACK|NEXT_NOT_FOUND/);
  });
});

describe("applications", () => {
  it("says so plainly when nothing has been tracked yet", async () => {
    stubBackend({ "/api/applications": fixtures.emptyPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams() }));

    expect(
      screen.getByText("No application has been recorded yet. Open a job and track it."),
    ).toBeInTheDocument();
  });

  it("passes the status filter to the API", async () => {
    const fetchMock = stubBackend({ "/api/applications": fixtures.emptyPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams({ status: "APPLIED" }) }));

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.searchParams.get("status")).toBe("APPLIED");
  });
});
