# ADR 0001 - Modular monolith and module boundaries

- Status: accepted
- Date: 2026-09-03

## Context

The system has to do ten fairly different jobs: talk to unreliable providers, keep raw
evidence, resolve identity, deduplicate, extract structure, score, serve search, run a
CRM, expose health, and stay observable. Each of those is a plausible service boundary,
and splitting them early would be the conventional resume-driven move.

It would also be wrong here. The pipeline stages share one transactional unit of work: a
single ingested record touches companies, jobs, sources, sightings, snapshots and run
counters, and any of those failing should roll the record back into `ingestion_failures`
rather than leave half of it committed across three databases. Distributed transactions
would be the price of an architecture nothing in the requirements asks for.

## Decision

One Spring Boot application, one PostgreSQL database, module boundaries expressed as Java
packages under `com.kji` with a rule about which way dependencies may point.

```
com.kji
  common/          Clock, id generation, hashing, URL canonicalization, text utilities
  source/          SourceAdapter SPI, source registry, health, rate limits, circuit breaker
    adapter/ats/     Greenhouse, Ashby, Lever and other public ATS boards
    adapter/imported/ NDJSON import boundary for out-of-band collectors
  ingest/          RawJobRecord, pipeline stages, SearchRun, ingestion failures
  snapshot/        JobSnapshot persistence, payload and content hashing
  company/         Company, aliases, provider identifiers, resolution, metrics, risk
  job/             Job, JobSource, JobSighting, verification, lifecycle transitions
  normalize/       Locale-aware normalization, Korean lexicons, experience and date parsing
  dedupe/          Match strategy ladder, merge decisions, confidence, manual correction
  intelligence/    Structured extraction with per-field confidence and evidence
  scoring/         Career value, candidate fit, application priority
  crm/             Applications and status history
  search/          Query model, full-text search, filters, sorts, facets
  web/             REST controllers and DTOs
  config/          Typed properties, Redis, metrics, security for internal endpoints
```

Dependency rules, checked in a test rather than trusted:

1. `common` depends on nothing else in `com.kji`.
2. `web` may depend on any module. No module may depend on `web`.
3. `source` and its adapters may not depend on `job`, `company`, `dedupe` or
   `intelligence`. An adapter's only output is a `RawJobRecord`, so a new provider can be
   added without touching the domain.
4. `ingest` orchestrates; it may depend on `source`, `snapshot`, `normalize`, `company`,
   `job`, `dedupe` and `intelligence`. None of those may depend on `ingest`.
5. `scoring` depends on `intelligence` and `job`, never the reverse. Scores are derived
   data and are recomputed, never authoritative.
6. `crm` depends on `job` only through its identifier.

## Consequences

- A provider is added by writing one adapter and one registry row. Nothing downstream
  changes, and the adapter can be tested against a recorded fixture with no database.
- The whole pipeline runs in one transaction per record, so a partial record is impossible
  and a failed record is recoverable from its persisted payload.
- The package boundaries are the seam if a stage ever has to move out. The realistic first
  candidate is crawling, because it is the stage whose latency and failure profile differs
  most from the rest; nothing else in the requirements justifies the move.
- Enforcement is a build-time test, so the boundaries decay visibly rather than silently.
