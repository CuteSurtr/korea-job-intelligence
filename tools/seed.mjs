#!/usr/bin/env node
/**
 * Loads the collected fixtures in `collected/` into a running backend, so a fresh stack has
 * something to look at.
 *
 * A migrated database is empty apart from the source registry, which means every console page
 * renders an empty state and there is nothing to test against. This walks the same import
 * boundary a real collector would: map each collected file to import-schema NDJSON, then POST
 * it to /api/internal/ingestion/import. Nothing here bypasses normalization, deduplication or
 * scoring, so what lands in the database is what the pipeline actually produces.
 *
 *   node tools/seed.mjs
 *   node tools/seed.mjs --backend http://localhost:8080 --only pathsdog,saramin
 *
 * The internal token is read from INTERNAL_API_TOKEN, or from a local .env if present.
 */
import { existsSync, readdirSync, readFileSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { argv, env, exit } from "node:process";

import { toNdjsonLines } from "./mcp-export/build-ndjson.mjs";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const COLLECTED = join(ROOT, "collected");
const IMPORTS = join(ROOT, "imports");

function parseArgs() {
  const options = { backend: env.BACKEND_URL ?? "http://localhost:8080", only: null, timeout: 120 };
  for (let i = 2; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--backend") {
      options.backend = argv[++i];
    } else if (arg === "--only") {
      options.only = argv[++i].split(",").map((value) => value.trim()).filter(Boolean);
    } else if (arg === "--timeout") {
      options.timeout = Number(argv[++i]);
    } else if (arg === "--help" || arg === "-h") {
      console.log("usage: node tools/seed.mjs [--backend URL] [--only a,b] [--timeout SECONDS]");
      exit(0);
    } else {
      console.error(`unknown argument: ${arg}`);
      exit(2);
    }
  }
  options.backend = options.backend.replace(/\/+$/, "");
  return options;
}

/** Reads INTERNAL_API_TOKEN from the environment, falling back to a local .env. */
function readToken() {
  if (env.INTERNAL_API_TOKEN) {
    return env.INTERNAL_API_TOKEN.trim();
  }
  const envFile = join(ROOT, ".env");
  if (!existsSync(envFile)) {
    return null;
  }
  for (const line of readFileSync(envFile, "utf8").split("\n")) {
    const match = /^\s*(?:export\s+)?INTERNAL_API_TOKEN\s*=\s*(.*)$/.exec(line);
    if (match) {
      return match[1].trim().replace(/^["']|["']$/g, "");
    }
  }
  return null;
}

async function waitForBackend(backend, timeoutSeconds) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let lastError = "no attempt made";
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${backend}/actuator/health`);
      if (response.ok) {
        return;
      }
      lastError = `health returned ${response.status}`;
    } catch (error) {
      lastError = String(error);
    }
    await new Promise((done) => setTimeout(done, 2000));
  }
  throw new Error(
    `${backend} did not become healthy within ${timeoutSeconds}s (${lastError}).\n` +
      "Start it with `docker compose up -d`, or `cd backend && ./gradlew bootRun`.",
  );
}

function collectedFiles(only) {
  if (!existsSync(COLLECTED)) {
    throw new Error(`no collected/ directory at ${COLLECTED}`);
  }
  const files = readdirSync(COLLECTED)
    .filter((name) => name.endsWith(".json"))
    .sort();
  if (!only) {
    return files;
  }
  const chosen = files.filter((name) => only.includes(name.replace(/\.json$/, "")));
  const missing = only.filter((name) => !files.includes(`${name}.json`));
  if (missing.length > 0) {
    throw new Error(`no collected file for: ${missing.join(", ")}`);
  }
  return chosen;
}

async function importOne(backend, token, provider, collector, body) {
  const url = new URL(`${backend}/api/internal/ingestion/import`);
  url.searchParams.set("source", provider);
  if (collector) {
    url.searchParams.set("collector", collector);
  }
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-ndjson", "X-Internal-Token": token },
    body,
  });
  const text = await response.text();
  if (response.status === 401) {
    throw new Error(
      "the backend rejected the internal token (401).\n" +
        "Set one on both sides and restart the backend so it picks it up:\n" +
        '  printf \'INTERNAL_API_TOKEN=%s\\n\' "$(openssl rand -hex 32)" > .env\n' +
        "  docker compose up -d",
    );
  }
  if (!response.ok) {
    throw new Error(`import failed with ${response.status}: ${text.slice(0, 400)}`);
  }
  return JSON.parse(text);
}

async function main() {
  const options = parseArgs();
  const token = readToken();
  if (!token) {
    console.error(
      "INTERNAL_API_TOKEN is not set, and the import endpoints answer 401 without it.\n" +
        "Create one, then restart the backend so it reads the same value:\n" +
        '  printf \'INTERNAL_API_TOKEN=%s\\n\' "$(openssl rand -hex 32)" > .env\n' +
        "  docker compose up -d",
    );
    exit(1);
  }

  console.log(`Waiting for ${options.backend} ...`);
  await waitForBackend(options.backend, options.timeout);

  mkdirSync(IMPORTS, { recursive: true });
  const files = collectedFiles(options.only);
  if (files.length === 0) {
    console.error("nothing to seed");
    exit(1);
  }

  const totals = { received: 0, normalized: 0, newJobs: 0, updated: 0, duplicates: 0, failures: 0 };
  const rows = [];

  for (const file of files) {
    const payload = JSON.parse(readFileSync(join(COLLECTED, file), "utf8"));
    let mapped;
    try {
      mapped = toNdjsonLines(payload);
    } catch (error) {
      console.error(`  ${file}: ${error.message}`);
      exit(1);
    }
    if (mapped.lines.length === 0) {
      console.log(`  ${mapped.provider}: nothing to import`);
      continue;
    }

    const body = mapped.lines.join("\n") + "\n";
    writeFileSync(join(IMPORTS, `${mapped.provider}.ndjson`), body, "utf8");

    const outcome = await importOne(
      options.backend,
      token,
      mapped.provider,
      mapped.collector,
      body,
    );
    totals.received += outcome.recordsReceived;
    totals.normalized += outcome.recordsNormalized;
    totals.newJobs += outcome.newJobs;
    totals.updated += outcome.updatedJobs;
    totals.duplicates += outcome.duplicates;
    totals.failures += outcome.failures;
    rows.push([mapped.provider, outcome.recordsReceived, outcome.newJobs, outcome.updatedJobs,
      outcome.duplicates, outcome.failures, outcome.status]);
  }

  const header = ["source", "received", "new", "updated", "merged", "failed", "status"];
  const widths = header.map((name, index) =>
    Math.max(name.length, ...rows.map((row) => String(row[index]).length)));
  const line = (cells) =>
    cells.map((cell, index) => String(cell).padEnd(widths[index])).join("  ").trimEnd();
  console.log("");
  console.log(line(header));
  console.log(widths.map((width) => "-".repeat(width)).join("  "));
  rows.forEach((row) => console.log(line(row)));

  const dashboard = await fetch(`${options.backend}/api/dashboard`).then((r) => r.json());
  console.log("");
  console.log(
    `${dashboard.totalJobs} jobs, ${totals.duplicates} merged as duplicates, ` +
      `${totals.failures} failures. Console: http://localhost:3000`,
  );
  if (totals.failures > 0) {
    console.log("Failed lines are stored in ingestion_failures; see /search-runs in the console.");
  }
}

main().catch((error) => {
  console.error(`\n${error.message}`);
  exit(1);
});
