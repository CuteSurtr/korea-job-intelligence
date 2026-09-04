import { describe, expect, it, vi } from "vitest";
import {
  BackendRequestError,
  BackendUnavailableError,
  fetchJson,
  isNotFound,
} from "../app/lib/api";

function respond(body: unknown, init?: { status?: number }) {
  const status = init?.status ?? 200;
  return new Response(typeof body === "string" ? body : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("fetchJson", () => {
  it("returns the parsed body on success", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => respond({ totalElements: 3 })));
    await expect(fetchJson<{ totalElements: number }>("/api/jobs")).resolves.toEqual({
      totalElements: 3,
    });
  });

  it("puts params on the query string and drops empty ones", async () => {
    const fetchMock = vi.fn(async (_input: unknown) => respond({}));
    vi.stubGlobal("fetch", fetchMock);

    await fetchJson("/api/jobs", {
      keyword: "engineer",
      size: 10,
      openOnly: false,
      company: "",
      state: undefined,
      location: null,
    });

    const url = fetchMock.mock.calls[0][0] as URL;
    expect(url.pathname).toBe("/api/jobs");
    expect(url.searchParams.get("keyword")).toBe("engineer");
    expect(url.searchParams.get("size")).toBe("10");
    // false is a meaningful filter value and must survive; empty and nullish must not appear
    expect(url.searchParams.get("openOnly")).toBe("false");
    expect(url.searchParams.has("company")).toBe(false);
    expect(url.searchParams.has("state")).toBe(false);
    expect(url.searchParams.has("location")).toBe(false);
  });

  it("reports an unreachable backend as unavailable, not as a bad response", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => {
      throw new TypeError("fetch failed");
    }));

    const error = await fetchJson("/api/jobs").catch((caught) => caught);
    expect(error).toBeInstanceOf(BackendUnavailableError);
    expect(isNotFound(error)).toBe(false);
  });

  it("carries the status through when the backend answers with an error", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => respond({ error: "server_error" }, { status: 500 })));

    const error = await fetchJson("/api/jobs").catch((caught) => caught);
    expect(error).toBeInstanceOf(BackendRequestError);
    expect((error as BackendRequestError).status).toBe(500);
    expect((error as BackendRequestError).path).toBe("/api/jobs");
    // a 500 is an error but not a missing row
    expect(isNotFound(error)).toBe(false);
  });

  it("recognises a 404 as a missing row rather than an outage", async () => {
    vi.stubGlobal("fetch", vi.fn(async () =>
      respond({ error: "not_found", message: "No job with id 999999" }, { status: 404 })));

    const error = await fetchJson("/api/jobs/999999").catch((caught) => caught);
    expect(isNotFound(error)).toBe(true);
    expect(error).not.toBeInstanceOf(BackendUnavailableError);
  });
});
