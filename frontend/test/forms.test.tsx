import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import JobDetailPage from "../app/jobs/[id]/page";
import ApplicationsPage from "../app/applications/page";
import ApplicationDetailPage from "../app/applications/[id]/page";

import {
  fixtures,
  renderPage,
  routeParams,
  searchParams,
  stubBackend,
  stubUnreachableBackend,
} from "./support";

const detailJob = fixtures.jobDetail.job;
const application = fixtures.application;

function field(name: string): HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement {
  const node = document.querySelector(`[name="${name}"]`);
  if (!node) {
    throw new Error(`no field named ${name}`);
  }
  return node as HTMLInputElement;
}

describe("tracking a job from its posting", () => {
  it("offers to start tracking when the job is not tracked yet", async () => {
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

    expect(screen.getByText("Not tracked yet")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Track" })).toBeInTheDocument();
    // the job travels with the form, since a server action gets nothing but the form data
    expect(field("jobId")).toHaveValue(String(detailJob.id));
    expect(field("status")).toHaveValue("INTERESTED");
  });

  it("offers to move the application when the job is already tracked", async () => {
    stubBackend({
      "/api/jobs": fixtures.jobDetail,
      "/api/applications": fixtures.trackedForJob,
    });
    await renderPage(
      JobDetailPage({
        params: routeParams({ id: String(detailJob.id) }),
        searchParams: searchParams(),
      }),
    );

    const tracked = fixtures.trackedForJob.content[0];
    expect(screen.getByRole("button", { name: "Move" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Track" })).not.toBeInTheDocument();
    // the select starts on the status it actually has, not on a default
    expect(field("status")).toHaveValue(tracked.status);
    expect(screen.getByRole("link", { name: "Open the application" })).toHaveAttribute(
      "href",
      `/applications/${tracked.id}`,
    );
  });

  it("still shows the posting when the tracking lookup fails", async () => {
    // The lookup is a convenience. Losing it must not take the job page down with it.
    stubBackend({
      "/api/jobs": fixtures.jobDetail,
      "/api/applications": { body: { error: "internal_error" }, status: 500 },
    });
    await renderPage(
      JobDetailPage({
        params: routeParams({ id: String(detailJob.id) }),
        searchParams: searchParams(),
      }),
    );

    expect(screen.getByRole("heading", { level: 1, name: detailJob.title })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Track" })).toBeInTheDocument();
    expect(screen.queryByText("The API rejected this request.")).not.toBeInTheDocument();
  });

  it("reports what went wrong after a write that failed", async () => {
    stubBackend({
      "/api/jobs": fixtures.jobDetail,
      "/api/applications": fixtures.untrackedForJob,
    });
    await renderPage(
      JobDetailPage({
        params: routeParams({ id: String(detailJob.id) }),
        searchParams: searchParams({ error: "No job with id 42" }),
      }),
    );

    expect(screen.getByRole("alert")).toHaveTextContent("No job with id 42");
  });
});

describe("a console with no token", () => {
  it("says it cannot write and disables the control, rather than failing on submit", async () => {
    // The test environment has no INTERNAL_API_TOKEN, which is the unconfigured console.
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

    expect(screen.getByRole("note")).toHaveTextContent("This console cannot write.");
    expect(screen.getByRole("note")).toHaveTextContent("INTERNAL_API_TOKEN");
    expect(screen.getByRole("button", { name: "Track" })).toBeDisabled();
  });

  it("disables saving on the record too", async () => {
    stubBackend({ "/api/applications": application });
    await renderPage(
      ApplicationDetailPage({
        params: routeParams({ id: String(application.id) }),
        searchParams: searchParams(),
      }),
    );

    expect(screen.getByRole("button", { name: "Save" })).toBeDisabled();
  });

  it("disables triage on the list too", async () => {
    stubBackend({ "/api/applications": fixtures.applicationsPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams() }));

    expect(screen.getAllByRole("button", { name: "Move" })[0]).toBeDisabled();
  });
});

describe("the applications list", () => {
  it("puts a status control on every row so triage does not need a page load each", async () => {
    stubBackend({ "/api/applications": fixtures.applicationsPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams() }));

    const row = screen.getAllByRole("row")[1];
    const select = within(row).getByRole("combobox");
    expect(select).toHaveValue(application.status);
    expect(within(row).getByRole("button", { name: "Move" })).toBeInTheDocument();
    expect(within(row).getByRole("link", { name: application.jobTitle })).toHaveAttribute(
      "href",
      `/applications/${application.id}`,
    );
  });

  it("carries the current filter back so a move does not drop it", async () => {
    stubBackend({ "/api/applications": fixtures.applicationsPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams({ status: "APPLIED" }) }));

    expect(field("returnTo")).toHaveValue("/applications?status=APPLIED");
  });

  it("says which filter is empty rather than claiming nothing is tracked", async () => {
    stubBackend({ "/api/applications": fixtures.emptyPage });
    await renderPage(ApplicationsPage({ searchParams: searchParams({ status: "OFFER" }) }));

    expect(screen.getByText("No application is offer.")).toBeInTheDocument();
  });

  it("confirms a move that succeeded", async () => {
    stubBackend({ "/api/applications": fixtures.applicationsPage });
    await renderPage(
      ApplicationsPage({ searchParams: searchParams({ saved: "Moved to rejected." }) }),
    );

    expect(screen.getByRole("status")).toHaveTextContent("Moved to rejected.");
  });
});

describe("the application record", () => {
  it("fills the form from what is stored", async () => {
    stubBackend({ "/api/applications": application });
    await renderPage(
      ApplicationDetailPage({
        params: routeParams({ id: String(application.id) }),
        searchParams: searchParams(),
      }),
    );

    expect(field("id")).toHaveValue(String(application.id));
    expect(field("status")).toHaveValue(application.status);
    expect(field("contactName")).toHaveValue(application.contactName ?? "");
    expect(field("notes")).toHaveValue(application.notes ?? "");
    // the note explains the next change, so it never carries the last one
    expect(field("note")).toHaveValue("");
  });

  it("shows an instant in a date input as a day", async () => {
    stubBackend({ "/api/applications": application });
    await renderPage(
      ApplicationDetailPage({
        params: routeParams({ id: String(application.id) }),
        searchParams: searchParams(),
      }),
    );

    const expected = new Date(application.appliedAt as string).toISOString().slice(0, 10);
    expect(field("appliedAt")).toHaveValue(expected);
    expect(field("appliedAt")).toHaveAttribute("type", "date");
  });

  it("renders every recorded status change with the status it came from", async () => {
    stubBackend({ "/api/applications": application });
    await renderPage(
      ApplicationDetailPage({
        params: routeParams({ id: String(application.id) }),
        searchParams: searchParams(),
      }),
    );

    const history = screen.getByRole("heading", { level: 2, name: "Status history" });
    expect(history).toBeInTheDocument();
    for (const change of application.history) {
      expect(screen.getAllByText(change.toStatus.toLowerCase().replace(/_/g, " ")).length)
        .toBeGreaterThan(0);
    }
  });

  it("raises a not-found for an application that does not exist", async () => {
    stubBackend({ "/api/applications": { body: { error: "not_found" }, status: 404 } });
    await expect(
      ApplicationDetailPage({
        params: routeParams({ id: "999999" }),
        searchParams: searchParams(),
      }),
    ).rejects.toThrow(/NEXT_HTTP_ERROR_FALLBACK|NEXT_NOT_FOUND/);
  });

  it("reports an outage as an outage", async () => {
    stubUnreachableBackend();
    await renderPage(
      ApplicationDetailPage({
        params: routeParams({ id: "1" }),
        searchParams: searchParams(),
      }),
    );

    expect(screen.getByText("The console could not reach the API.")).toBeInTheDocument();
  });
});
