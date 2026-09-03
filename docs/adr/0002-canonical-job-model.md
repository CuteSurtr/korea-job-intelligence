# ADR 0002 - Canonical job and source sighting model

- Status: accepted
- Date: 2026-09-03

## Context

A live probe of the available providers on 2026-09-03 returned the same opening more than
once within a single provider pair. Searching one Korean employer returned
`[Toss] Systems Engineer (GPU)` from JobKorea as record 1 and the same opening from
Saramin as record 52, with different external ids, different URL shapes, and different
renderings of the experience requirement (`experience 3 years or more` against
`experience 3 up-arrow`). A cross-company search on another provider returned the same
Coupang opening carrying a Greenhouse `gh_jid`, which is the employer's own ATS identifier
for it.

If each provider row becomes a job row, the job count inflates by the number of providers
that happen to carry the posting, and the count becomes a measure of provider overlap
rather than of opportunities. That is the opposite of what the system is for.

## Decision

`Job` is the canonical opening and owns nothing provider-specific. Everything a provider
asserts hangs off it:

- `job_sources` is one row per (job, source) pair and holds that provider's external id,
  its URLs, its own first and last seen timestamps, and how it was matched to the job.
- `job_sightings` is an append-only observation log. One row per observation, whether or
  not anything changed. This is what makes "when did this posting actually appear and
  disappear on each provider" answerable.
- `job_snapshots` holds the raw payload, written when the content hash changes rather than
  on every sighting. Snapshots are never updated in place.

A job's canonical fields are chosen by source trust tier, not by recency: an employer ATS
board outranks an aggregator that links to it, which outranks a provider that returns only
a snippet. `sources.trust_tier` records this, so promoting a provider is a data change.

Identity keys that a provider hands over are kept in `company_identifiers`, keyed by type
and value, so a Greenhouse board token, a LinkedIn company slug and a Korean board's
company id all resolve to one `Company` without any of them being privileged as the
primary key.

## Consequences

- Job counts mean opportunities. Provider overlap shows up as `source_count` on a job,
  which is useful signal rather than noise.
- Deleting a provider deletes its `job_sources` and sightings and leaves the canonical job
  intact, with its history of having been seen there.
- A job that was only ever seen on one low-trust provider is representable and visibly
  weaker than one corroborated by the employer's own board, without a separate quality
  flag.
- The cost is that job resolution has to run on every ingested record. ADR 0006 covers the
  evidence ladder that keeps it from merging things it should not.
