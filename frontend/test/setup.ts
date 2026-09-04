import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach, vi } from "vitest";

// The pages read the backend through fetch. Every test states the responses it expects, so a
// test that forgets one fails loudly rather than reaching a real backend.
beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: unknown) => {
      throw new Error(`unstubbed fetch: ${String(input)}`);
    }),
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});
