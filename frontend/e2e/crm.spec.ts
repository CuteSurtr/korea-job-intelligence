import { expect, test, type Page } from "@playwright/test";

const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";

async function api<T>(path: string): Promise<T> {
  const response = await fetch(`${BACKEND}${path}`);
  if (!response.ok) {
    throw new Error(`${path} answered ${response.status}`);
  }
  return (await response.json()) as T;
}

interface Job {
  id: number;
  title: string;
}

interface Application {
  id: number;
  jobId: number;
  status: string;
  resumeVersion: string | null;
  contactName: string | null;
  contactEmail: string | null;
  interviewStage: string | null;
  notes: string | null;
  followUpAt: string | null;
  history: { fromStatus: string | null; toStatus: string; note: string | null }[];
}

/** A job nothing is tracking yet, so each run starts from the untracked state. */
async function untrackedJob(): Promise<Job> {
  const jobs = await api<{ content: Job[] }>("/api/jobs?size=100&sort=NEWEST");
  const applications = await api<{ content: Application[] }>("/api/applications?size=200");
  const taken = new Set(applications.content.map((application) => application.jobId));
  const job = jobs.content.find((candidate) => !taken.has(candidate.id));
  if (!job) {
    throw new Error("every seeded job is already tracked; reseed before running this");
  }
  return job;
}

/** Submits a form and waits for the action's redirect to land. */
async function submit(page: Page, click: Promise<void>, expected: RegExp) {
  await Promise.all([page.waitForURL(expected), click]);
}

test("a job is tracked, edited and triaged entirely from the console", async ({ page }) => {
  const job = await untrackedJob();

  await test.step("the posting offers to start tracking", async () => {
    await page.goto(`/jobs/${job.id}`);
    await expect(page.locator(".track-label")).toHaveText(/not tracked yet/i);
    await expect(page.locator('.track-form button')).toHaveText("Track");
  });

  await test.step("tracking it opens the application it created", async () => {
    await page.selectOption('.track-form select[name="status"]', "READY_TO_APPLY");
    await page.fill('.track-form input[name="note"]', "found in the console");
    await submit(page, page.click(".track-form button"), /\/applications\/\d+\?saved=/);
    await expect(page.getByRole("status")).toContainText("Tracking as ready to apply");
  });

  const applicationId = Number(/applications\/(\d+)/.exec(page.url())![1]);

  await test.step("the record carries what was just written", async () => {
    await expect(page.locator('select[name="status"]')).toHaveValue("READY_TO_APPLY");
    // the creating transition is already in the history, with the note that explained it
    await expect(page.getByRole("cell", { name: "found in the console" })).toBeVisible();
  });

  await test.step("the whole record saves in one submit", async () => {
    await page.selectOption('select[name="status"]', "INTERVIEW");
    await page.fill('input[name="note"]', "recruiter call booked");
    await page.fill('input[name="resumeVersion"]', "v3");
    await page.fill('input[name="contactName"]', "Jae");
    await page.fill('input[name="contactEmail"]', "jae@example.com");
    await page.fill('input[name="interviewStage"]', "phone screen");
    await page.fill('textarea[name="notes"]', "strong match");
    await page.fill('input[name="followUpAt"]', "2026-09-17");
    await submit(page, page.click(".form-actions button"), /saved=Saved/);

    const saved = await api<Application>(`/api/applications/${applicationId}`);
    expect(saved.status).toBe("INTERVIEW");
    expect(saved.resumeVersion).toBe("v3");
    expect(saved.contactName).toBe("Jae");
    expect(saved.contactEmail).toBe("jae@example.com");
    expect(saved.interviewStage).toBe("phone screen");
    expect(saved.notes).toBe("strong match");
    // the date input said 2026-09-17; the API stores instants
    expect(saved.followUpAt).toBe("2026-09-17T00:00:00Z");
    expect(saved.history.map((change) => change.toStatus)).toEqual([
      "READY_TO_APPLY",
      "INTERVIEW",
    ]);
    expect(saved.history[1].note).toBe("recruiter call booked");
  });

  await test.step("triage from the list moves the status and nothing else", async () => {
    await page.goto("/applications");
    const row = page.locator("table tbody tr").filter({ hasText: job.title }).first();
    await row.locator("select").selectOption("REJECTED");
    await submit(page, row.locator("button").click(), /saved=Moved/);

    const moved = await api<Application>(`/api/applications/${applicationId}`);
    expect(moved.status).toBe("REJECTED");
    // the fields the edit form filled must survive a status move made from the list
    expect(moved.resumeVersion).toBe("v3");
    expect(moved.contactName).toBe("Jae");
    expect(moved.history.map((change) => change.toStatus)).toContain("REJECTED");
  });

  await test.step("a move made from a filtered list returns to that filter", async () => {
    await page.goto("/applications?status=REJECTED");
    const row = page.locator("table tbody tr").filter({ hasText: job.title }).first();
    await row.locator("select").selectOption("WITHDRAWN");
    await submit(page, row.locator("button").click(), /status=REJECTED.*saved=/);
  });

  await test.step("the posting now offers to move rather than to track", async () => {
    await page.goto(`/jobs/${job.id}`);
    await expect(page.locator(".track-label")).toHaveText(/tracked as withdrawn/i);
    await expect(page.locator(".track-form button")).toHaveText("Move");
    await expect(page.locator(".track-form a")).toHaveAttribute(
      "href",
      `/applications/${applicationId}`,
    );
  });
});

test("the console reports a write the API refused", async ({ page }) => {
  // A job id the API does not have: the console must say what happened, not swallow it.
  await page.goto("/jobs/999999999");
  expect(page.url()).toContain("/jobs/999999999");
  await expect(page.getByRole("heading", { level: 1, name: "Not found" })).toBeVisible();
});
