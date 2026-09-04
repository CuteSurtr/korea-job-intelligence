import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const TOKEN = "5f2b8c1d4e6a7b9c0d1e2f3a4b5c6d7e";
const original = process.env.INTERNAL_API_TOKEN;

beforeEach(() => {
  vi.resetModules();
});

afterEach(() => {
  if (original === undefined) {
    delete process.env.INTERNAL_API_TOKEN;
  } else {
    process.env.INTERNAL_API_TOKEN = original;
  }
});

/** Imports the client fresh, so it reads whatever the environment currently says. */
async function client() {
  return import("../app/lib/api");
}

function ok(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("writing to a guarded API", () => {
  it("sends the internal token on a write when the console has one", async () => {
    process.env.INTERNAL_API_TOKEN = TOKEN;
    const { sendJson } = await client();
    const fetchMock = vi.fn(async (_input: unknown, _init?: RequestInit) => ok({ id: 1 }));
    vi.stubGlobal("fetch", fetchMock);

    await sendJson("/api/applications", "POST", { jobId: 1 });

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Record<string, string>;
    expect(headers["X-Internal-Token"]).toBe(TOKEN);
    expect(headers["Content-Type"]).toBe("application/json");
  });

  it("never puts the token on a read", async () => {
    process.env.INTERNAL_API_TOKEN = TOKEN;
    const { fetchJson } = await client();
    const fetchMock = vi.fn(async (_input: unknown, _init?: RequestInit) => ok({ content: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await fetchJson("/api/applications");

    const init = fetchMock.mock.calls[0][1] as RequestInit | undefined;
    const headers = (init?.headers ?? {}) as Record<string, string>;
    expect(headers["X-Internal-Token"]).toBeUndefined();
  });

  it("still sends the write when no token is set, and lets the API refuse it", async () => {
    delete process.env.INTERNAL_API_TOKEN;
    const { sendJson } = await client();
    const fetchMock = vi.fn(async (_input: unknown, _init?: RequestInit) => ok({ id: 1 }));
    vi.stubGlobal("fetch", fetchMock);

    await sendJson("/api/applications", "POST", { jobId: 1 });

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Record<string, string>;
    // the console does not invent a token; the API is the one that decides
    expect(headers["X-Internal-Token"]).toBeUndefined();
  });

  it("knows whether it can write", async () => {
    process.env.INTERNAL_API_TOKEN = TOKEN;
    expect((await client()).canWrite()).toBe(true);

    vi.resetModules();
    process.env.INTERNAL_API_TOKEN = "   ";
    expect((await client()).canWrite()).toBe(false);

    vi.resetModules();
    delete process.env.INTERNAL_API_TOKEN;
    expect((await client()).canWrite()).toBe(false);
  });
});

describe("explaining a refused write", () => {
  it("names the missing setting when the console has no token", async () => {
    delete process.env.INTERNAL_API_TOKEN;
    const { apiMessage, BackendRequestError } = await client();

    const message = apiMessage(
      new BackendRequestError("/api/applications", 401, '{"error":"unauthorized"}'),
    );
    expect(message).toContain("INTERNAL_API_TOKEN");
    expect(message).toContain("no INTERNAL_API_TOKEN");
  });

  it("says the tokens disagree when the console has one and it was rejected", async () => {
    process.env.INTERNAL_API_TOKEN = TOKEN;
    const { apiMessage, BackendRequestError } = await client();

    const message = apiMessage(
      new BackendRequestError("/api/applications", 401, '{"error":"unauthorized"}'),
    );
    expect(message).toContain("rejected this console's internal token");
    // it must not leak the token itself into a message rendered on a page
    expect(message).not.toContain(TOKEN);
  });

  it("still passes other API messages through unchanged", async () => {
    process.env.INTERNAL_API_TOKEN = TOKEN;
    const { apiMessage, BackendRequestError } = await client();

    const message = apiMessage(
      new BackendRequestError("/api/applications", 400, '{"message":"No job with id 42"}'),
    );
    expect(message).toBe("No job with id 42");
  });
});
