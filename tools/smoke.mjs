#!/usr/bin/env node
/**
 * Asserts that a running console and a running backend agree.
 *
 * The unit suites test each side against a stub, which cannot catch the two halves drifting
 * apart: a renamed DTO field, a route that 404s, a page that renders its error panel because
 * the API answered something it did not expect. This drives the real console over HTTP
 * against a real backend holding real seeded rows, and fails if any page comes back empty,
 * broken or apologising.
 *
 *   node tools/smoke.mjs
 *   node tools/smoke.mjs --frontend http://localhost:3000 --backend http://localhost:8080
 */
import { readFileSync } from "node:fs";
import { argv, env, exit } from "node:process";

/** Reads a setting from the environment, falling back to a local .env. */
function readSetting(name) {
  if (env[name]) {
    return env[name].trim();
  }
  const envFile = new URL("../.env", import.meta.url);
  try {
    for (const line of readFileSync(envFile, "utf8").split("\n")) {
      const match = new RegExp(`^\\s*(?:export\\s+)?${name}\\s*=\\s*(.*)$`).exec(line);
      if (match) {
        return match[1].trim().replace(/^["']|["']$/g, "");
      }
    }
  } catch {
    // no .env; the checks that need it will say so
  }
  return null;
}

function readToken() {
  return readSetting("INTERNAL_API_TOKEN");
}

/** Where the stack is published: an explicit URL wins, else the ports Compose maps. */
function origin(urlVar, portVar, fallbackPort) {
  if (env[urlVar]) {
    return env[urlVar];
  }
  return `http://localhost:${readSetting(portVar) || fallbackPort}`;
}

function parseArgs() {
  const options = {
    frontend: origin("FRONTEND_URL", "FRONTEND_PORT", "3000"),
    backend: origin("BACKEND_URL", "BACKEND_PORT", "8080"),
    timeout: 180,
  };
  for (let i = 2; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--frontend") options.frontend = argv[++i];
    else if (arg === "--backend") options.backend = argv[++i];
    else if (arg === "--timeout") options.timeout = Number(argv[++i]);
    else if (arg === "--help" || arg === "-h") {
      console.log("usage: node tools/smoke.mjs [--frontend URL] [--backend URL] [--timeout S]");
      exit(0);
    } else {
      console.error(`unknown argument: ${arg}`);
      exit(2);
    }
  }
  options.frontend = options.frontend.replace(/\/+$/, "");
  options.backend = options.backend.replace(/\/+$/, "");
  return options;
}

const checks = [];
let failed = 0;

function record(name, error) {
  if (error) {
    failed += 1;
    checks.push(`  FAIL  ${name}\n        ${error}`);
  } else {
    checks.push(`  ok    ${name}`);
  }
}

async function check(name, run) {
  try {
    await run();
    record(name, null);
  } catch (error) {
    record(name, error instanceof Error ? error.message : String(error));
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function waitFor(url, timeoutSeconds, label) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let last = "no attempt made";
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
      last = `returned ${response.status}`;
    } catch (error) {
      last = String(error);
    }
    await new Promise((done) => setTimeout(done, 2000));
  }
  throw new Error(`${label} at ${url} never became ready (${last})`);
}

/** Text the console renders when it cannot show real content. Any of it means a failed page. */
const APOLOGIES = [
  "The console could not reach the API.",
  "The API rejected this request.",
  "Application error",
  "This page could not be found",
];

async function page(frontend, path) {
  const response = await fetch(`${frontend}${path}`);
  assert(response.status === 200, `${path} returned ${response.status}, expected 200`);
  const html = await response.text();
  for (const apology of APOLOGIES) {
    assert(!html.includes(apology), `${path} rendered "${apology}"`);
  }
  return html;
}

async function main() {
  const options = parseArgs();
  console.log(`console  ${options.frontend}`);
  console.log(`backend  ${options.backend}\n`);

  await waitFor(`${options.backend}/actuator/health`, options.timeout, "backend");
  await waitFor(`${options.frontend}/health`, options.timeout, "console");

  const json = async (path) => {
    const response = await fetch(`${options.backend}${path}`);
    assert(response.ok, `${path} returned ${response.status}`);
    return response.json();
  };

  // The API must be holding seeded rows, or every page below would pass while showing nothing.
  const dashboard = await json("/api/dashboard");
  assert(dashboard.totalJobs > 0,
    "the backend has no jobs; run `node tools/seed.mjs` before the smoke test");
  const jobs = await json("/api/jobs?size=1");
  assert(jobs.content.length > 0, "/api/jobs returned no rows");
  const jobId = jobs.content[0].id;
  const companyId = jobs.content[0].companyId;
  const runs = await json("/api/search-runs?size=1");
  assert(runs.content.length > 0, "/api/search-runs returned no rows");
  const runId = runs.content[0].id;

  console.log(`seeded: ${dashboard.totalJobs} jobs, ${dashboard.sourceCount} sources\n`);

  await check("GET /api/jobs/{id} returns the evidence chain", async () => {
    const detail = await json(`/api/jobs/${jobId}`);
    assert(detail.job?.id === jobId, "job detail did not carry the job");
    assert("sources" in detail, "job detail had no sources");
  });

  await check("GET /api/companies/{id} resolves", async () => {
    const company = await json(`/api/companies/${companyId}`);
    assert(company.id === companyId, "company detail returned the wrong row");
  });

  await check("GET /api/sources/health reports every source", async () => {
    const health = await json("/api/sources/health");
    assert(Array.isArray(health) && health.length > 0, "no source health rows");
  });

  await check("a missing job is 404, not 500", async () => {
    const response = await fetch(`${options.backend}/api/jobs/999999999`);
    assert(response.status === 404, `expected 404, got ${response.status}`);
  });

  await check("internal ingestion refuses an unauthenticated call", async () => {
    const response = await fetch(`${options.backend}/api/internal/ingestion/import?source=indeed`, {
      method: "POST",
      headers: { "Content-Type": "application/x-ndjson" },
      body: "",
    });
    assert(response.status === 401, `expected 401, got ${response.status}`);
  });

  await check("dashboard renders its counters", async () => {
    const html = await page(options.frontend, "/");
    assert(html.includes("Open jobs"), "no open-jobs stat");
  });

  await check("jobs list renders rows", async () => {
    const html = await page(options.frontend, "/jobs");
    assert(html.includes("canonical postings"), "no posting count");
    assert(html.includes(`/jobs/${jobId}`), `no link to job ${jobId}`);
  });

  await check("jobs list applies a filter", async () => {
    await page(options.frontend, "/jobs?sort=JUNIOR_FRIENDLY&maxYearsExperience=3");
  });

  await check("job detail renders", async () => {
    const html = await page(options.frontend, `/jobs/${jobId}`);
    assert(html.includes(`/companies/${companyId}`), "job detail did not link its company");
  });

  await check("job detail offers to track the posting", async () => {
    const html = await page(options.frontend, `/jobs/${jobId}`);
    assert(html.includes("track-form"), "no tracking form on the job page");
    // a server action receives nothing but the form, so the job has to travel inside it
    assert(html.includes('name="jobId"'), "the tracking form does not carry the job");
  });

  await check("companies list renders", async () => {
    const html = await page(options.frontend, "/companies");
    assert(html.includes(`/companies/${companyId}`), `no link to company ${companyId}`);
  });

  await check("company detail renders", () => page(options.frontend, `/companies/${companyId}`));
  await check("sources renders", () => page(options.frontend, "/sources"));
  await check("search runs list renders", async () => {
    const html = await page(options.frontend, "/search-runs");
    assert(html.includes(`/search-runs/${runId}`), `no link to run ${runId}`);
  });
  await check("search run detail renders", () => page(options.frontend, `/search-runs/${runId}`));
  await check("applications renders", async () => {
    const html = await page(options.frontend, "/applications");
    assert(html.includes('name="status"'), "no status control on the applications page");
  });

  await check("an unauthenticated write to the CRM is refused", async () => {
    const response = await fetch(`${options.backend}/api/applications`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ jobId, status: "INTERESTED" }),
    });
    assert(response.status === 401, `expected 401, got ${response.status}`);
  });

  await check("reading the CRM stays open", async () => {
    const listed = await json("/api/applications");
    assert(Array.isArray(listed.content), "the application list did not come back");
  });

  await check("the CRM write path round-trips through the API", async () => {
    const token = readToken();
    assert(token, "INTERNAL_API_TOKEN is not set, and the API requires it to write");
    const write = (path, method, payload) =>
      fetch(`${options.backend}${path}`, {
        method,
        headers: { "Content-Type": "application/json", "X-Internal-Token": token },
        body: JSON.stringify(payload),
      });

    const created = await write("/api/applications", "POST",
      { jobId, status: "INTERESTED", note: "smoke test" });
    assert(created.ok, `creating an application answered ${created.status}`);
    const application = await created.json();

    const moved = await write(`/api/applications/${application.id}`, "PATCH",
      { status: "APPLIED", note: "smoke test" });
    assert(moved.ok, `moving an application answered ${moved.status}`);
    const after = await moved.json();
    assert(after.status === "APPLIED", `status stayed ${after.status}`);
    // both transitions have to be on the record, which is the point of tracking at all
    assert(after.history.length >= 2, `history recorded ${after.history.length} changes`);

    const lookup = await json(`/api/applications?jobId=${jobId}`);
    assert(lookup.totalElements === 1, "the job lookup did not find the application just created");
  });

  await check("the console shows the application on its record page", async () => {
    const lookup = await json(`/api/applications?jobId=${jobId}`);
    const application = lookup.content[0];
    const html = await page(options.frontend, `/applications/${application.id}`);
    assert(html.includes("Status history"), "the record page has no status history");
    assert(html.includes('name="resumeVersion"'), "the record page has no edit form");
  });

  await check("a missing job in the console is 404, not a fake outage", async () => {
    const response = await fetch(`${options.frontend}/jobs/999999999`);
    assert(response.status === 404, `expected 404, got ${response.status}`);
    const html = await response.text();
    // The regression this guards: a reachable backend answering 404 used to render the
    // "could not reach the API, start the stack" panel with a 200.
    assert(!html.includes("The console could not reach the API."),
      "a missing job still claims the backend is unreachable");
  });

  await check("a missing company in the console is 404", async () => {
    const response = await fetch(`${options.frontend}/companies/999999999`);
    assert(response.status === 404, `expected 404, got ${response.status}`);
  });

  console.log(checks.join("\n"));
  console.log(`\n${checks.length - failed}/${checks.length} checks passed`);
  if (failed > 0) {
    exit(1);
  }
}

main().catch((error) => {
  console.error(`\nsmoke test could not run: ${error.message}`);
  exit(1);
});
