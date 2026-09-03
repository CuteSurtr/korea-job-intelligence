# korea-job-intelligence

A backend platform that discovers South Korean software engineering openings across many
providers, resolves them into canonical jobs, tracks their lifecycle with source-backed
evidence, and ranks them for a specific candidate.

## Table of contents

- [What the system does](#what-the-system-does)
- [Architecture](#architecture)
- [Two source pathways](#two-source-pathways)
- [Ingestion pipeline](#ingestion-pipeline)
- [Domain model](#domain-model)
- [Deduplication](#deduplication)
- [Job lifecycle](#job-lifecycle)
- [Scoring](#scoring)
- [Provenance rules](#provenance-rules)
- [Results from a real run](#results-from-a-real-run)
- [Running it](#running-it)
- [Configuration](#configuration)
- [API](#api)
- [Testing and CI](#testing-and-ci)
- [Repository layout](#repository-layout)
- [Documentation](#documentation)

## What the system does

A single opening is often visible on an employer ATS board, on one or more Korean job boards,
and on international aggregators, each with its own identifier, its own title casing, and its
own idea of what "3 years" means. The platform treats the opening as one canonical `Job` with
many `JobSource` rows behind it, keeps the raw provider payload that every conclusion was
drawn from, and refuses to state anything it cannot trace back to a snapshot.

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
- Prometheus metrics, a Grafana dashboard, and run-correlated structured logs

## Architecture

A modular monolith. Java 21, Spring Boot, PostgreSQL as the single source of truth, Redis as
an optional accelerator, Flyway for schema, Gradle for builds, Testcontainers for integration
tests, Next.js for the operator console.

```mermaid
flowchart LR
    subgraph ATS["Employer ATS boards"]
        GH["Greenhouse"]
        AB["Ashby"]
        LV["Lever"]
    end

    subgraph OOB["Providers the backend cannot reach"]
        PD["Pathsdog"]
        KB["JobKorea / Saramin"]
        AG["LinkedIn / Indeed / freehire"]
    end

    COL["Out-of-band collector"]
    ND["NDJSON file"]

    subgraph APP["Spring Boot modular monolith"]
        AD["Source adapters"]
        IMP["Import endpoint"]
        PIPE["Ingestion pipeline"]
        API["REST API"]
    end

    PG[("PostgreSQL<br/>source of truth")]
    RD[("Redis<br/>optional cache")]
    UI["Next.js console"]
    PROM["Prometheus"]
    GRAF["Grafana"]

    GH --> AD
    AB --> AD
    LV --> AD
    PD --> COL
    KB --> COL
    AG --> COL
    COL --> ND
    ND --> IMP
    AD --> PIPE
    IMP --> PIPE
    PIPE --> PG
    API --> PG
    API -.-> RD
    UI --> API
    APP -- "/actuator/prometheus" --> PROM
    PROM --> GRAF
```

There is deliberately no Kafka, no Kubernetes and no Elasticsearch. PostgreSQL full-text
search plus `pg_trgm` covers the search and fuzzy-matching requirements at this data volume;
the reasoning and the conditions that would change it are in
[ADR 0004](docs/adr/0004-postgres-first-search.md). Module boundaries are Java packages under
`com.kji`, described in [ADR 0001](docs/adr/0001-modular-monolith.md) and enforced by a test.

## Two source pathways

Providers the deployed backend can call are queried in process. Everything else is collected
out of band and crosses a durable boundary as NDJSON. Both pathways build the same
`RawJobRecord`, so nothing downstream can tell them apart, and the runtime never depends on a
collector existing. See [ADR 0003](docs/adr/0003-import-boundary.md).

```mermaid
flowchart TB
    A["Public ATS API"] --> B["SourceAdapter.fetch"]
    C["Any collector, any language, offline"] --> D["NDJSON, one record per line"]
    D --> E["POST /api/internal/ingestion/import"]
    B --> F["RawJobRecord"]
    E --> F
    F --> G["The same pipeline for every source"]
```

`sources.runtime_available` records which providers the backend can query for itself. Only
those are eligible for scheduled direct runs; the rest are import-only, and the distinction
is visible in `/api/sources` rather than buried in configuration. It is a statement about
reachability, not about quality.

## Ingestion pipeline

```mermaid
flowchart LR
    F["FETCH"] --> S["RAW SNAPSHOT"] --> P["PARSE"] --> N["NORMALIZE"]
    N --> CR["COMPANY RESOLUTION"] --> JR["JOB RESOLUTION"] --> DD["DEDUPLICATION"]
    DD --> IN["INTELLIGENCE"] --> V["VERIFICATION"] --> PE["PERSIST"] --> IX["INDEX"] --> RK["RANK"]
```

Every run is a `SearchRun` recording source, query, timings, records received and normalized,
new and updated jobs, duplicates, failures and rate-limit events. Each record is ingested in
its own transaction, so a partial record is impossible and a failed one is recoverable:
malformed records are written to `ingestion_failures` with their payload rather than dropped.

## Domain model

```mermaid
erDiagram
    COMPANY ||--o{ JOB : employs
    COMPANY ||--o{ COMPANY_ALIAS : "known as"
    COMPANY ||--o{ COMPANY_IDENTIFIER : "identified on"
    COMPANY ||--o{ COMPANY_METRIC : "measured by"
    COMPANY ||--o{ COMPANY_RISK_REASON : "assessed by"

    JOB ||--o{ JOB_SOURCE : "seen on"
    JOB ||--o{ JOB_SIGHTING : observed
    JOB ||--o{ JOB_VERIFICATION : "evidenced by"
    JOB ||--o{ JOB_LIFECYCLE_EVENT : "transitioned by"
    JOB ||--|| JOB_INTELLIGENCE : "extracted into"
    JOB ||--o{ JOB_INTELLIGENCE_FIELD : "field evidence"
    JOB ||--o{ JOB_SKILL : requires
    JOB ||--o{ JOB_SCORE : scored
    JOB ||--o{ APPLICATION : "applied to"

    JOB_SOURCE ||--o{ JOB_SNAPSHOT : "raw payload"
    SOURCE ||--o{ JOB_SOURCE : provides
    SOURCE ||--|| SOURCE_HEALTH : "reliability of"
    SOURCE ||--o{ SEARCH_RUN : "run against"
    SEARCH_RUN ||--o{ INGESTION_FAILURE : records
    APPLICATION ||--o{ APPLICATION_STATUS_HISTORY : "moved through"
    CANDIDATE_PROFILE ||--o{ JOB_SCORE : "fit for"
    CANDIDATE_PROFILE ||--o{ APPLICATION : "submitted by"
```

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

## Deduplication

Merging is destructive in a way that splitting is not. The ladder takes the first rung that
fires and records the rung, the confidence and the evidence on every attachment.

```mermaid
flowchart TB
    R1{"Same canonical apply URL?"} -->|yes| M1["merge, confidence 1.00"]
    R1 -->|no| R2{"Same ATS provider and external id?"}
    R2 -->|yes| M2["merge, confidence 0.98"]
    R2 -->|no| G{"Does the target already carry a row<br/>from this same source?"}
    G -->|yes| Q["review queue, both jobs kept"]
    G -->|no| R3{"Same company, title and<br/>compatible location?"}
    R3 -->|yes| M3["merge, confidence 0.85"]
    R3 -->|no| R4{"Same company and high<br/>description similarity?"}
    R4 -->|yes| M4["merge, confidence 0.75"]
    R4 -->|no| NEW["new canonical job"]
```

The guard in the middle is the rule that a provider listing two postings is that provider
asserting two postings. Weaker title or description similarity does not overturn it. It was
added after a live run merged six separately numbered `Server Developer` openings from one
Greenhouse board into a single job. Full reasoning in
[ADR 0006](docs/adr/0006-deduplication-ladder.md).

## Job lifecycle

A failed fetch is not a closed posting. Closure requires evidence, and the evidence is stored
and referenced from the job.

```mermaid
stateDiagram-v2
    [*] --> DISCOVERED: first sighting
    DISCOVERED --> ACTIVE: seen in a successful listing
    ACTIVE --> UNVERIFIED: no confirmation past the horizon
    UNVERIFIED --> STALE: still unconfirmed
    UNVERIFIED --> ACTIVE: seen again
    STALE --> ACTIVE: seen again
    ACTIVE --> CLOSED: absent from a complete listing,<br/>404 on the canonical posting,<br/>or a passed deadline
    STALE --> CLOSED: same evidence
    CLOSED --> REOPENED: observed present again
    REOPENED --> CLOSED: closed again on evidence
```

A timeout, a connection failure, a 5xx, a 429, an empty response, a parse failure or an open
circuit advances nothing. Absence is only evidence within the listing scope actually fetched,
so a complete run against one ATS board says nothing about another board on the same ATS.
Full reasoning in [ADR 0005](docs/adr/0005-lifecycle-evidence.md).

## Scoring

Three scores, computed and stored separately, because a role can be excellent engineering
experience and still be closed to a new graduate.

```mermaid
flowchart LR
    I["JobIntelligence<br/>skills, role family, seniority"] --> CV["CAREER_VALUE<br/>property of the job"]
    I --> CF["CANDIDATE_FIT<br/>keyed by profile"]
    CV --> AP["APPLICATION_PRIORITY"]
    CF --> AP
    D["Deadline urgency"] --> AP
    RK["Company risk"] --> AP
    AP --> SORT["default sort order"]
```

Every score row stores `score_version`, `component_scores` as JSON and a human-readable
explanation built from the components that actually moved it. A score without its components
cannot be written. Weights live in a versioned JSON resource, so a reweighting is a version
bump and a recompute. See [ADR 0007](docs/adr/0007-scoring-separation.md).

## Provenance rules

These are enforced in code and covered by tests:

1. Unknown beats an unsupported inference. A field with no evidence stays null.
2. A failed fetch is not a closed posting.
3. `first_seen_at` is never overwritten.
4. Normalized values never overwrite the raw payload they were derived from.
5. Two postings merge only on evidence strong enough to name; every merge stores the method,
   the confidence and the evidence, and can be corrected by hand.

Asking why the system believes something is a join:

```sql
SELECT f.field_name, f.field_value, f.confidence, f.extraction_method,
       f.evidence_text, s.source_url, s.fetched_at, src.code
FROM job_intelligence_fields f
JOIN job_snapshots s ON s.id = f.evidence_snapshot_id
JOIN sources src     ON src.id = s.source_id
WHERE f.job_id = :jobId AND f.field_name = 'years_experience_min';
```

## Results from a real run

From one run against an empty database on 2026-09-03, across nine employer ATS boards on
three ATS platforms and six imported providers:

| | |
| --- | ---: |
| Canonical jobs | 971 |
| Provider rows behind them | 1017 |
| Companies resolved | 80 |
| Raw snapshots | 1017 |
| Jobs corroborated by more than one source | 45 |
| Pairs queued for review | 120 |
| Records discarded | 0 |

500 of the 971 jobs have no seniority bucket and 478 no role family, stored as unknown rather
than guessed. Filtering to what the system is for gives 137 junior-accessible open jobs.

The run exposed three defects that no amount of fixture testing would have found: a complete
listing for one board closing another board's jobs, the deduplication ladder overruling an
employer about its own catalogue, and two spellings of a root path producing two canonical
keys. All three are fixed with regression tests and written up in
[the run log](docs/ingestion-run-2026-09-03.md).

## Running it

Everything in containers:

```bash
cp .env.example .env
docker compose up -d --build
```

The console is on `http://localhost:3000` and the API on `http://localhost:8080`. Add the
observability stack when you want it:

```bash
docker compose --profile observability up -d
```

Prometheus lands on `http://localhost:9090` and Grafana on `http://localhost:3001`, with the
datasource and the ingestion dashboard provisioned.

For backend development, run the dependencies in Docker and the application from source:

```bash
docker compose up -d postgres redis
cd backend && ./gradlew bootRun
cd frontend && npm install && npm run dev
```

Trigger a direct ingestion run against an employer board:

```bash
curl -X POST http://localhost:8080/api/internal/ingestion/run \
  -H "X-Internal-Token: $INTERNAL_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"source":"greenhouse","parameters":{"board":"daangn"},"maxRecords":500}'
```

Import records collected out of band:

```bash
node tools/mcp-export/build-ndjson.mjs collected/pathsdog.json imports/pathsdog.ndjson
curl -X POST "http://localhost:8080/api/internal/ingestion/import?source=pathsdog" \
  -H "X-Internal-Token: $INTERNAL_API_TOKEN" \
  -H "Content-Type: application/x-ndjson" \
  --data-binary @imports/pathsdog.ndjson
```

## Configuration

All settings have working defaults; `.env.example` lists every one. The ones worth knowing:

| Variable | Default | Purpose |
| --- | --- | --- |
| `INTERNAL_API_TOKEN` | none | Required by every `/api/internal/**` endpoint. Generate your own |
| `CACHE_ENABLED` | `true` | Redis is an accelerator; the system serves correctly without it |
| `INGESTION_SCHEDULER_ENABLED` | `false` | Scheduled direct runs against configured targets |
| `INGESTION_STALE_AFTER` | `P14D` | Staleness horizon for unverified jobs |
| `SOURCE_CIRCUIT_FAILURE_THRESHOLD` | `5` | Consecutive failures before a source's circuit opens |
| `FRONTEND_PORT`, `BACKEND_PORT` | `3000`, `8080` | Published host ports, so the stack fits around what you already run |

Scheduled ingestion targets are declared in `application.yml` under `kji.ingestion.targets`,
so adding a board is configuration rather than code.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/jobs` | Filtered, sorted canonical job list |
| GET | `/api/jobs/{id}` | Canonical job with sources, intelligence, scores and evidence |
| GET | `/api/companies` | Company list |
| GET | `/api/companies/{id}` | Company with aliases, identifiers, metrics and risk reasons |
| GET | `/api/search` | Same query model as `/api/jobs` |
| GET | `/api/sources` | Source registry with runtime availability and trust tier |
| GET | `/api/sources/health` | Per-source latency, failures, rate limits, circuit state |
| GET | `/api/search-runs` | Ingestion run history |
| GET | `/api/search-runs/{id}` | One run with counters and its recorded failures |
| POST | `/api/internal/ingestion/import` | NDJSON import boundary |
| POST | `/api/internal/ingestion/run` | Trigger a direct-source run |
| GET, POST, PATCH | `/api/applications` | Application CRM with status history |
| GET | `/api/dashboard` | Aggregate counters |
| GET | `/actuator/health`, `/actuator/prometheus` | Health and metrics |

Filters on the job list: `keyword`, `company`, `state`, `location`, `source`, `roleFamily`,
`seniority`, `maxYearsExperience`, `minCareerValue`, `minCandidateFit`, `remotePolicy`,
`degreeRequired`, `companyRisk`, `postedWithinDays`, `openOnly`. Sorts: `BEST_MATCH`,
`HIGHEST_CAREER_VALUE`, `JUNIOR_FRIENDLY`, `NEWEST`, `CLOSING_SOON`, `RECENTLY_VERIFIED`,
`MOST_SOURCES`, `COMPANY`.

## Testing and CI

98 tests. Provider adapters run against recorded fixtures, so CI never depends on a live job
site. Everything else runs against real PostgreSQL through Testcontainers.

```bash
cd backend && ./gradlew clean build
```

What the suite deliberately covers: normalization of Korean legal-form and experience
variants, deadline sentinels, deduplication including the same-source guard, company
resolution, lifecycle transitions, `first_seen_at` preservation, source-outage behaviour,
closure evidence, reopening, circuit opening, provenance from a claim back to its snapshot,
scoring separation and explanation, cache degradation with Redis absent, and the module
dependency rules.

Coverage is verified in the build, not merely reported: `jacocoTestCoverageVerification` runs
as part of `check` with floors at 75% line and 50% branch against 80.7% and 57.8% measured.

Two workflows. Backend: build, test, coverage summary, artifacts, image build, then start the
image against a real PostgreSQL to prove it migrates and serves. Frontend: typecheck,
production dependency audit, build, image build.

## Repository layout

```
backend/     Spring Boot modular monolith
frontend/    Next.js operator console
docs/        Architecture decision records, schema, source coverage, run log
ops/         Prometheus and Grafana configuration
tools/       Out-of-band collectors that emit NDJSON for the import boundary
collected/   Provider-shaped input for the recorded run, replayable
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
