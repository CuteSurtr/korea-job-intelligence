import type { ReactElement } from "react";
import { render } from "@testing-library/react";
import { vi } from "vitest";
import fixtures from "./fixtures/api.json";

export { fixtures };

type Route = { body: unknown; status?: number };

/**
 * Answers the pages' fetches from fixtures.
 *
 * Routes are matched by pathname prefix, longest first, so `/api/jobs/42` can be given a
 * different answer from `/api/jobs`. Any path with no route fails the test rather than
 * silently returning nothing.
 */
export function stubBackend(routes: Record<string, Route | unknown>) {
  const entries = Object.entries(routes)
    .map(([path, value]) => {
      const route = (value && typeof value === "object" && "body" in (value as Route)
        ? (value as Route)
        : { body: value }) as Route;
      return [path, route] as const;
    })
    .sort(([a], [b]) => b.length - a.length);

  const fetchMock = vi.fn(async (input: unknown) => {
    const url = input instanceof URL ? input : new URL(String(input));
    const match =
      entries.find(([path]) => url.pathname === path) ??
      entries.find(([path]) => url.pathname.startsWith(`${path}/`));
    if (!match) {
      throw new Error(`no fixture for ${url.pathname}`);
    }
    const [, route] = match;
    return new Response(JSON.stringify(route.body), {
      status: route.status ?? 200,
      headers: { "Content-Type": "application/json" },
    });
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** Makes the backend unreachable, the way a stopped stack behaves. */
export function stubUnreachableBackend() {
  const fetchMock = vi.fn(async () => {
    throw new TypeError("fetch failed");
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** Renders an async server component by awaiting it into an element first. */
export async function renderPage(page: Promise<ReactElement> | ReactElement) {
  return render(await page);
}

/** The searchParams shape App Router hands a page. */
export function searchParams(values: Record<string, string> = {}) {
  return Promise.resolve(values);
}

/** The route params shape App Router hands a dynamic page, keeping the literal key types. */
export function routeParams<T extends Record<string, string>>(values: T): Promise<T> {
  return Promise.resolve(values);
}
