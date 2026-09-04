import { defineConfig } from "@playwright/test";

/**
 * Drives the console in a real browser against an already-running stack.
 *
 * The forms post to server actions, and a server action only runs when the browser submits
 * the form Next rendered. Nothing short of a browser exercises that wiring, which is why this
 * exists alongside the component tests rather than instead of them.
 *
 * Start the stack and seed it first; this does not manage either.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "list" : "line",
  use: {
    baseURL: process.env.FRONTEND_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
    // Some environments already ship a Chromium and cannot download Playwright's own build.
    // Point PLAYWRIGHT_CHROMIUM_PATH at that binary to run against it instead.
    ...(process.env.PLAYWRIGHT_CHROMIUM_PATH
      ? { launchOptions: { executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH } }
      : {}),
  },
});
