import { beforeEach, describe, expect, it, vi } from "vitest";

const revalidatePath = vi.fn();

/** Stands in for Next's redirect, which throws to unwind the action. */
class Redirected extends Error {
  readonly to: string;
  constructor(to: string) {
    super(`redirect: ${to}`);
    this.name = "Redirected";
    this.to = to;
  }
}

vi.mock("next/cache", () => ({ revalidatePath: (path: string) => revalidatePath(path) }));
vi.mock("next/navigation", () => ({
  redirect: (to: string) => {
    throw new Redirected(to);
  },
}));

const { trackJob, saveApplication, setApplicationStatus } = await import("../app/lib/actions");

function form(values: Record<string, string>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(values)) {
    data.set(key, value);
  }
  return data;
}

/** The message a redirect carries back, decoded the way a browser would read it. */
function flash(to: string, key: "error" | "saved"): string {
  return new URL(to, "http://local").searchParams.get(key) ?? "";
}

function ok(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Runs an action and returns where it redirected plus what it sent. */
async function run(
  action: (data: FormData) => Promise<void>,
  data: FormData,
  response: Response = ok({ id: 7 }),
) {
  const fetchMock = vi.fn(async () => response);
  vi.stubGlobal("fetch", fetchMock);

  let to = "";
  try {
    await action(data);
    throw new Error("the action returned without redirecting");
  } catch (error) {
    if (!(error instanceof Redirected)) {
      throw error;
    }
    to = error.to;
  }

  const call = fetchMock.mock.calls[0] as unknown as [URL, RequestInit] | undefined;
  return {
    to,
    calls: fetchMock.mock.calls.length,
    url: call?.[0],
    method: call?.[1]?.method,
    sent: call ? (JSON.parse(String(call[1].body)) as Record<string, unknown>) : null,
  };
}

beforeEach(() => {
  revalidatePath.mockClear();
});

describe("trackJob", () => {
  it("posts the job and status, then opens the application it created", async () => {
    const result = await run(trackJob, form({ jobId: "42", status: "INTERESTED", note: "fits" }));

    expect(result.method).toBe("POST");
    expect(result.url?.pathname).toBe("/api/applications");
    expect(result.sent).toEqual({ jobId: 42, status: "INTERESTED", note: "fits" });
    expect(result.to).toBe("/applications/7?saved=Tracking+as+interested.");
  });

  it("leaves the note out when it is blank rather than recording an empty one", async () => {
    const result = await run(trackJob, form({ jobId: "42", status: "INTERESTED", note: "   " }));
    expect(result.sent).toEqual({ jobId: 42, status: "INTERESTED" });
  });

  it("refuses a status the API does not have, without calling it", async () => {
    const result = await run(trackJob, form({ jobId: "42", status: "MAYBE" }));

    expect(result.calls).toBe(0);
    expect(result.to).toContain("/jobs/42?error=");
    expect(flash(result.to, "error")).toBe('"MAYBE" is not an application status.');
  });

  it("refuses a form with no job", async () => {
    const result = await run(trackJob, form({ jobId: "", status: "INTERESTED" }));
    expect(result.calls).toBe(0);
    expect(result.to).toContain("/jobs?error=");
  });

  it("shows what the API said when it refuses the write", async () => {
    const result = await run(
      trackJob,
      form({ jobId: "42", status: "INTERESTED" }),
      ok({ error: "invalid_request", message: "No job with id 42" }, 400),
    );

    expect(result.to).toContain("/jobs/42?error=");
    expect(flash(result.to, "error")).toBe("No job with id 42");
    // a failed write must not claim the list changed
    expect(revalidatePath).not.toHaveBeenCalled();
  });

  it("refreshes the pages that show the application after a successful write", async () => {
    await run(trackJob, form({ jobId: "42", status: "INTERESTED" }));
    expect(revalidatePath.mock.calls.flat()).toEqual(
      expect.arrayContaining(["/jobs/42", "/applications", "/"]),
    );
  });
});

describe("saveApplication", () => {
  it("patches every field on the form", async () => {
    const result = await run(
      saveApplication,
      form({
        id: "7",
        status: "INTERVIEW",
        note: "recruiter call booked",
        resumeVersion: "v3",
        coverLetterVersion: "v1",
        contactName: "Jae",
        contactEmail: "jae@example.com",
        referral: "a former colleague",
        interviewStage: "phone screen",
        interviewNotes: "45 minutes, systems design",
        notes: "strong match",
      }),
    );

    expect(result.method).toBe("PATCH");
    expect(result.url?.pathname).toBe("/api/applications/7");
    expect(result.sent).toMatchObject({
      status: "INTERVIEW",
      note: "recruiter call booked",
      resumeVersion: "v3",
      contactEmail: "jae@example.com",
      interviewStage: "phone screen",
      notes: "strong match",
    });
    expect(result.to).toBe("/applications/7?saved=Saved.");
  });

  it("turns a date input into an instant the API can read", async () => {
    const result = await run(
      saveApplication,
      form({ id: "7", status: "APPLIED", appliedAt: "2026-09-10", followUpAt: "2026-09-17" }),
    );

    expect(result.sent?.appliedAt).toBe("2026-09-10T00:00:00Z");
    expect(result.sent?.followUpAt).toBe("2026-09-17T00:00:00Z");
  });

  it("omits a blank date instead of sending an empty string the API cannot parse", async () => {
    const result = await run(
      saveApplication,
      form({ id: "7", status: "APPLIED", appliedAt: "", followUpAt: "" }),
    );

    expect(result.sent).not.toHaveProperty("appliedAt");
    expect(result.sent).not.toHaveProperty("followUpAt");
  });

  it("sends a cleared text field as empty, which is how the API clears it", async () => {
    const result = await run(
      saveApplication,
      form({ id: "7", status: "APPLIED", contactName: "" }),
    );

    // an absent field means "leave it alone"; an empty one means "make it empty"
    expect(result.sent?.contactName).toBe("");
  });

  it("refuses an unknown status without calling the API", async () => {
    const result = await run(saveApplication, form({ id: "7", status: "SOMEDAY" }));
    expect(result.calls).toBe(0);
    expect(result.to).toContain("/applications/7?error=");
  });

  it("reports a 404 from the API rather than pretending it saved", async () => {
    const result = await run(
      saveApplication,
      form({ id: "7", status: "APPLIED" }),
      ok({ error: "not_found", message: "No application with id 7" }, 404),
    );

    expect(flash(result.to, "error")).toBe("No application with id 7");
  });
});

describe("setApplicationStatus", () => {
  it("sends the status alone so triage cannot wipe the other fields", async () => {
    const result = await run(
      setApplicationStatus,
      form({ id: "7", status: "REJECTED", returnTo: "/applications?status=APPLIED" }),
    );

    expect(result.method).toBe("PATCH");
    expect(result.sent).toEqual({ status: "REJECTED" });
  });

  it("returns to the filtered list it was used from", async () => {
    const result = await run(
      setApplicationStatus,
      form({ id: "7", status: "REJECTED", returnTo: "/applications?status=APPLIED" }),
    );

    expect(result.to).toContain("/applications");
    expect(result.to).toContain("status=APPLIED");
    expect(result.to).toContain("saved=");
  });

  it("falls back to the unfiltered list when no return was given", async () => {
    const result = await run(setApplicationStatus, form({ id: "7", status: "OFFER" }));
    expect(result.to.startsWith("/applications?")).toBe(true);
  });
});
