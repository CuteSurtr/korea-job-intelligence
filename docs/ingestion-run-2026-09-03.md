# First full ingestion run, 2026-09-03

A record of what actually happened when the pipeline was pointed at real Korean sources for
the first time, including the two defects it exposed. The numbers below are from a run
against an empty database.

## Direct sources

The backend queried eight employer ATS boards itself. Five of the eight board tokens were
discovered by an aggregator that names the underlying ATS for each posting, which is the
main practical argument for keeping low-trust aggregators in the registry: they lead to
high-trust boards.

| Board | Source | Received | New jobs | Duplicates | Failures |
| --- | --- | ---: | ---: | ---: | ---: |
| `tosscareers` | greenhouse | 566 | 566 | 0 | 0 |
| `daangn` | greenhouse | 39 | 39 | 0 | 0 |
| `furiosaai` | greenhouse | 49 | 49 | 0 | 0 |
| `seoulrobotics` | greenhouse | 11 | 11 | 0 | 0 |
| `lgairesearch` | greenhouse | 42 | 42 | 0 | 0 |
| `get-delight` | greenhouse | 4 | 4 | 0 | 0 |
| `vessl-ai` | ashby | 17 | 17 | 0 | 0 |
| `twelve-labs` | ashby | 17 | 17 | 0 | 0 |

## Import boundary

Six providers were collected out of band, mapped to NDJSON by
[`tools/mcp-export`](../tools/mcp-export/README.md) and posted to the import endpoint.

| Source | Received | New jobs | Merged into an existing job | Failures |
| --- | ---: | ---: | ---: | ---: |
| `pathsdog` | 30 | 30 | 0 | 0 |
| `jobkorea` | 64 | 63 | 1 | 0 |
| `saramin` | 137 | 102 | 35 | 0 |
| `linkedin` | 10 | 10 | 0 | 0 |
| `indeed` | 10 | 10 | 0 | 0 |
| `freehire` | 20 | 10 | 10 | 0 |

## Result

| | |
| --- | ---: |
| Canonical jobs | 970 |
| Provider rows behind them | 1016 |
| Companies resolved | 79 |
| Raw snapshots | 1016 |
| Skills extracted | 1443 |
| Scores computed | 2910 |
| Pairs queued for review | 120 |
| Records discarded | 0 |

Forty-five jobs are corroborated by more than one source. One is corroborated by three: a
당근마켓 posting that appears on the employer's own Greenhouse board and on both JobKorea and
Saramin, resolved to one job with three provider rows behind it. Nine merges came from the
canonical URL rung and thirty-seven from company, title and location; nothing merged on
description similarity, and nothing merged inside a single provider's own catalogue.

## What the system says it does not know

500 of 970 jobs have no seniority bucket and 478 have no role family. Most of those are the
non-engineering postings on a large employer's board, where the title carries no role signal
and the extractor found no phrase to read. They are stored as unknown rather than guessed,
which is why the average career-value score across all 970 jobs is 7.1: the corpus is mostly
not software engineering, and the scorer says so.

Filtering to what the system is for gives 137 open jobs in seniority buckets A and B. Sorted
by career value, the top of that list is a 당근마켓 backend internship at 78, corroborated by
two sources, with every extracted field traceable to snapshot 1004 and the phrase it was read
from.

## Two defects this run exposed

**A complete listing for one board closed another board's jobs.** Reconciliation was scoped to
the source rather than to the listing actually fetched, so a successful, complete run against
an 11-posting board concluded that 264 postings on a different board of the same ATS had
disappeared. Absence is only evidence within the scope that was actually listed.
`SourceFetchResult` now carries the scope it covered and reconciliation is confined to it,
with a regression test that ingests one board and asserts the other board's jobs are
untouched.

**Rung 3 overruled an employer about its own catalogue.** On the 566-posting board, company
plus normalized title plus city merged 98 postings, collapsing six separately numbered
`Server Developer` openings into one job. Greenhouse had given each its own id and its own
URL. Rungs 3 and 4 are now cross-source only; when the target job already carries a row from
the same source, the pair goes to the review queue instead. That is where 120 of the 120
pending pairs come from.

A third, smaller flaw showed up in the same data: a URL whose path was empty and the same URL
whose path was `/` produced different canonical keys, so an aggregator row that should have
matched on rung 1 fell through to rung 3. Both now canonicalize identically.
