# Source coverage record

Every row below reflects a tool that was actually invoked, with the query used and what
came back. A provider is not listed as covered on the strength of its name.

## Probe log

Probes run 2026-09-03 from the development workstation.

| Provider | Tool invoked | Query | Result |
| --- | --- | --- | --- |
| Pathsdog | `search_jobs` | `experience=new-grad`, `skills=[Backend]`, `limit=3` | 3 records, each with company, skills, experience label, deadline, a Pathsdog URL and the employer's original URL |
| Pathsdog | `get_job_detail` | `job_id=2565` | `internal_error`, marked retryable. Search works; detail was unavailable at probe time |
| JobKorea + Saramin | `search_jobs` | `company=Toss`, `platform=all` | 60 records across both boards, including cross-board duplicates of the same opening |
| LinkedIn | `linkedin_search_jobs` | `keyword=backend engineer`, `location=South Korea`, `posted_within=Past month` | 10 records with company, company URL, numeric id, location, posted date |
| Indeed | `search_jobs` | `search=backend developer`, `location=Seoul`, `country=KR` | 10 records; ids are response-scoped, URLs are shortener redirects |
| freehire | `freehire_search_jobs` | `countries=kr`, `q=backend`, `reality=[fresh]`, `limit=3` | 3 of 28 records, each carrying the underlying ATS name, the ATS external id, canonical skills, seniority and work mode |
| openings company search | `search_jobs_by_company` | `company=coupang` | Not in the roster; returned nearest slugs. Company coverage is slug-gated |
| Greenhouse board API | `GET boards-api.greenhouse.io/v1/boards/coupang/jobs` | direct HTTP | HTTP 200, full JSON board |
| Ashby board API | `GET api.ashbyhq.com/posting-api/job-board/vessl-ai` | direct HTTP | HTTP 200, full JSON board |
| JobDataLake | `authenticate` | not invoked | Requires an OAuth flow the user has not completed. Unavailable, not covered |

## What the probes established about the data

These findings are the reason several design decisions look the way they do.

**Cross-provider duplication is real and immediate.** The Toss search returned
`[Toss] Systems Engineer (GPU)` from JobKorea and again from Saramin, and
`[Toss] Cafe Barista` likewise, with different external ids and different URL shapes.
Deduplication is not a theoretical requirement here.

**Company names differ by single characters.** The same employer came back as two strings
that differ only in which parenthesis characters were used for the Korean legal-form
marker, and is also known by a short brand name and a romanized name. Exact matching on the
company string fails on the first record.

**Provider experience fields are not trustworthy as numbers.** The same board returned
`experience 100 years or more` on a barista posting and `experience 0 years or more` on an
assistant posting. Any parse of these must record what it read and its confidence, and must
be willing to answer unknown.

**Deadline sentinels exist.** Multiple records carried `2069-12-31`, which is a
continuous-hiring sentinel rather than a date. ADR 0005 handles this with a nullable
deadline plus an explicit open-ended flag.

**The same field is rendered differently per board.** One board returned
`experience 3 years or more` where the other returned `experience 3` with an up-arrow, and
one returned opaque region codes such as `I150` where the other returned
`Seoul Gangnam-gu`. Normalization is per-source, not global.

**Aggregators expose the employer's own ATS.** freehire returned `source: greenhouse` with
`external_id: coupang:8168878` and `source: ashby` with a board slug and posting UUID.
Those board APIs answered directly over plain HTTP, which is what makes the direct pathway
in ADR 0003 real rather than aspirational.

**Aggregator URLs carry tracking parameters.** freehire appends `utm_source` to the
employer's own URL, so the same posting has two different URL strings. URL canonicalization
is load-bearing for rung 1 of the deduplication ladder.

**One provider's ids are not durable.** The Indeed tool returned ids of the form
`JOBSEARCH_1871` numbered sequentially within the response, and apply links as shortener
redirects. That provider is registered with `stable_external_id = false` and is skipped at
rung 2 of the ladder.

## Full-run coverage, 2026-09-03

Every source in the table below was invoked and its records were carried through the whole
pipeline into the database. The counts and the two defects the run exposed are recorded in
[the run log](ingestion-run-2026-09-03.md).

| Source | Pathway | Provider rows ingested |
| --- | --- | ---: |
| `greenhouse` | direct, 6 boards | 711 |
| `saramin` | import | 137 |
| `jobkorea` | import | 64 |
| `ashby` | direct, 2 boards | 34 |
| `pathsdog` | import | 30 |
| `freehire` | import | 20 |
| `indeed` | import | 10 |
| `linkedin` | import | 10 |
| `lever` | direct, 1 board (`aleph`, filtered to Seoul) | 1 |
| `jobdatalake` | not invoked, authentication not completed | 0 |

Five of the eight employer board tokens queried directly (`tosscareers`, `daangn`,
`furiosaai`, `seoulrobotics`, `get-delight`) were discovered from freehire rows that name the
underlying ATS and its external id. That is the practical case for keeping a tier-2 aggregator
in the registry: it is a route to tier-1 boards.

## Registry consequences

| Source | Adapter kind | Runtime available | Trust tier | Stable external id | Full description |
| --- | --- | --- | --- | --- | --- |
| `greenhouse` | ATS | yes | 1 | yes | yes |
| `ashby` | ATS | yes | 1 | yes | yes |
| `lever` | ATS | yes | 1 | yes | yes |
| `pathsdog` | import | no | 2 | yes | on detail fetch |
| `jobkorea` | import | no | 2 | yes | no |
| `saramin` | import | no | 2 | yes | no |
| `freehire` | import | no | 2 | yes | partial |
| `linkedin` | import | no | 3 | yes | no |
| `indeed` | import | no | 3 | no | no |
| `jobdatalake` | import | no | 3 | unknown | unknown |

`runtime_available = no` means the deployed backend cannot query the provider itself and
the data arrives through the import boundary in ADR 0003. It is not a statement about the
provider's quality.
