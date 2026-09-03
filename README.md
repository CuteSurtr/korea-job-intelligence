# korea-job-intelligence

A backend platform that discovers South Korean software engineering openings across many
providers, resolves them into canonical jobs, tracks their lifecycle with source-backed
evidence, and ranks them for a specific candidate.

## Table of contents

- [What the system does](#what-the-system-does)
- [Architecture](#architecture)
- [Domain model](#domain-model)
- [Ingestion pipeline](#ingestion-pipeline)
- [Provenance rules](#provenance-rules)
- [Running locally](#running-locally)
- [API](#api)
- [Repository layout](#repository-layout)
- [Documentation](#documentation)

## What the system does

A single opening is often visible on an employer ATS board, on one or more Korean job
boards, and on international aggregators, each with its own identifier, its own title
casing, and its own idea of what "3 years" means. The platform treats the opening as one
canonical `Job` with many `JobSource` rows behind it, keeps the raw provider payload that
every conclusion was drawn from, and refuses to state anything it cannot trace back to a
snapshot.

Capabilities:

- Multi-source aggregation across employer ATS APIs and job boards
- Canonical job resolution and evidence-ranked deduplication
- Lifecycle tracking that distinguishes a closed posting from a failed fetch
- Structured job intelligence with per-field confidence and evidence
- Explainable SWE career-value scoring, kept separate from candidate fit
- Company intelligence with sourced, timestamped metrics
- Application CRM with full status history
- PostgreSQL-backed search, filtering and analytics
- Per-source health, rate-limit state and circuit breaking
- Prometheus metrics, Grafana dashboards and run-correlated structured logs

## Architecture

A modular monolith. Java 21, Spring Boot, PostgreSQL as the single source of truth, Redis
as an optional accelerator, Flyway for schema, Gradle for builds, Testcontainers for
integration tests, Next.js for the operator UI.

Module boundaries live in Java packages under `com.kji` and are described in
[ADR 0001](docs/adr/0001-modular-monolith.md).

There is deliberately no Kafka, no Kubernetes and no Elasticsearch. PostgreSQL full-text
search plus `pg_trgm` covers the search and fuzzy-matching requirements at this data
volume; the reasoning and the conditions that would change it are recorded in
[ADR 0004](docs/adr/0004-postgres-first-search.md).

## Domain model

| Entity | Responsibility |
| --- | --- |
| `Company` | Employer identity, aliases, per-provider identifiers |
| `Job` | Canonical opening, independent of where it was discovered |
| `JobSource` | One provider's view of a canonical job, with its external id |
| `JobSighting` | Append-only log of every observation of a job on a source |
| `JobSnapshot` | Immutable raw provider payload plus extracted raw fields |
| `JobVerification` | Evidence that a posting was present, absent or unreachable |
| `JobIntelligence` | Structured extraction with per-field confidence and evidence |
| `JobScore` | Career value, candidate fit and application priority |
| `CompanyMetric` | Sourced, timestamped company facts |
| `Application` | Candidate application state and history |
| `SearchRun` | One ingestion run and its counters |
| `SourceHealth` | Per-source reliability, latency and circuit state |

## Ingestion pipeline

```
FETCH -> RAW SNAPSHOT -> PARSE -> NORMALIZE -> COMPANY RESOLUTION -> JOB RESOLUTION
      -> DEDUPLICATION -> INTELLIGENCE -> VERIFICATION -> PERSIST -> INDEX -> RANK
```

Two source pathways converge on the same `RawJobRecord`:

- **Direct**, for providers the deployed backend can call itself, such as public
  Greenhouse, Ashby and Lever boards.
- **Import**, for data collected out of band and handed over as NDJSON through
  `POST /api/internal/ingestion/import`. The runtime never depends on the collector being
  present; see [ADR 0003](docs/adr/0003-import-boundary.md).

Every run records source, query, timings, records received and normalized, new and updated
jobs, duplicates, failures and rate-limit events. Malformed records are persisted to
`ingestion_failures` with their payload rather than dropped.

## Provenance rules

These are enforced in code and covered by tests:

1. Unknown beats an unsupported inference. A field with no evidence stays null.
2. A failed fetch is not a closed posting. Closure requires a successful source response in
   which the posting is absent, an authoritative not-found on the canonical posting, or a
   passed deadline with applications unavailable.
3. `first_seen_at` is never overwritten.
4. Normalized values never overwrite the raw payload they were derived from.
5. Two postings merge only on evidence strong enough to name; every merge stores the
   method, the confidence and the evidence, and can be corrected by hand.

## Running locally

```bash
cp .env.example .env
docker compose up -d postgres redis
cd backend && ./gradlew bootRun
```

Full build including Testcontainers-backed integration tests:

```bash
cd backend && ./gradlew build
```

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/jobs` | Filtered, sorted canonical job list |
| GET | `/api/jobs/{id}` | Canonical job with sources, intelligence and evidence |
| GET | `/api/companies` | Company list |
| GET | `/api/companies/{id}` | Company with metrics and risk reasons |
| GET | `/api/search` | Full-text and faceted search |
| GET | `/api/sources` | Source registry |
| GET | `/api/sources/health` | Per-source reliability state |
| GET | `/api/search-runs` | Ingestion run history |
| GET | `/api/search-runs/{id}` | One run with counters and failures |
| POST | `/api/internal/ingestion/import` | NDJSON import boundary |
| POST | `/api/internal/ingestion/run` | Trigger a direct-source run |
| GET, POST, PATCH | `/api/applications` | Application CRM |
| GET | `/api/dashboard` | Aggregate counters |

## Repository layout

```
backend/     Spring Boot modular monolith
frontend/    Next.js operator UI
docs/        Architecture decision records and source coverage
ops/         Prometheus and Grafana configuration
tools/       Offline collectors that emit NDJSON for the import boundary
```

## Documentation

- [ADR 0001 - Modular monolith and module boundaries](docs/adr/0001-modular-monolith.md)
- [ADR 0002 - Canonical job and source sighting model](docs/adr/0002-canonical-job-model.md)
- [ADR 0003 - Durable import boundary for out-of-band collectors](docs/adr/0003-import-boundary.md)
- [ADR 0004 - PostgreSQL-first search and matching](docs/adr/0004-postgres-first-search.md)
- [ADR 0005 - Lifecycle states and closure evidence](docs/adr/0005-lifecycle-evidence.md)
- [ADR 0006 - Deduplication evidence ladder](docs/adr/0006-deduplication-ladder.md)
- [ADR 0007 - Scoring separation and explainability](docs/adr/0007-scoring-separation.md)
- [Source coverage record](docs/source-coverage.md)
- [First full ingestion run, with the defects it exposed](docs/ingestion-run-2026-09-03.md)
- [Database schema](docs/schema.md)
- [Out-of-band collectors and the import boundary](tools/mcp-export/README.md)
