# ADR 0003 - Durable import boundary for out-of-band collectors

- Status: accepted
- Date: 2026-09-03

## Context

Several of the highest-value Korean providers are reachable during development through
tooling that is attached to a developer workstation, not to the deployed application. That
tooling is a research instrument. It has no service contract, no uptime guarantee, and no
presence in production.

Coupling ingestion to it would produce a system that works on one laptop and nowhere else,
and whose data provenance depends on a session that no longer exists.

## Decision

Collectors that cannot run inside the backend write NDJSON and hand it over through a
durable boundary. One JSON object per line, one line per observed posting, conforming to
the `RawJobRecord` import schema.

```
collector (any tool, any language, offline)
    -> NDJSON file
    -> POST /api/internal/ingestion/import   (or the equivalent CLI)
    -> the same pipeline every other source uses
```

Rules:

1. The backend has no compile-time or runtime dependency on any collector. Removing every
   collector from the machine changes nothing about whether the application starts, serves
   or ingests.
2. An imported record is not privileged. It goes through the same parse, normalize,
   resolve, deduplicate and verify stages as a record the backend fetched itself, and it
   lands in the same `job_snapshots` table with the same hashing.
3. The import records who produced it. Each line carries `source_code`, `fetched_at` and
   `collector` so a snapshot can be traced to the run that produced it long after that run
   is gone.
4. An import is a `SearchRun` like any other, with the same counters, and its malformed
   lines land in `ingestion_failures` with the offending line preserved.
5. `sources.runtime_available` marks which providers the deployed backend can query for
   itself. Only those are eligible for scheduled direct runs; the rest are import-only, and
   the distinction is visible in `/api/sources` rather than buried in configuration.

Providers the backend can call directly, currently the public Greenhouse and Ashby board
APIs, are queried in-process and skip the file entirely. Both pathways construct the same
`RawJobRecord`, so nothing downstream can tell them apart.

## Consequences

- Ingested data outlives the tool that collected it, which is the property that makes
  provenance claims meaningful.
- The import file is replayable. A pipeline bug can be fixed and the same NDJSON reingested
  to produce corrected output from unchanged evidence.
- Import-only sources have lower freshness than direct sources, because they update when a
  collector runs rather than on a schedule. `source_health.last_success_at` exposes this
  instead of hiding it.
- Import is an authenticated internal endpoint. It accepts data that becomes provenance, so
  it is not open.
