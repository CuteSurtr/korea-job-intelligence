# ADR 0005 - Lifecycle states and closure evidence

- Status: accepted
- Date: 2026-09-03

## Context

The naive lifecycle rule is "absent from the last fetch means closed". It is wrong in the
common case. A provider timing out, rate-limiting, changing its response shape, or
returning an empty page under load all produce absence, and none of them is evidence that
an employer stopped hiring.

A closed job that is really open is the worse error of the two: it silently removes an
opportunity from view, and nothing about the UI reveals that it happened.

Korean boards make this harder in a specific way. A probe on 2026-09-03 returned postings
with a deadline of `2069-12-31`, which is a sentinel for continuous hiring, alongside real
dated deadlines. Treating that sentinel as a date makes those jobs immortal; treating any
far-future date as a sentinel makes real long deadlines vanish.

## Decision

Six states with named transitions:

| State | Meaning |
| --- | --- |
| `DISCOVERED` | Seen once; not yet corroborated or verified |
| `ACTIVE` | Seen in the most recent successful run of at least one source |
| `UNVERIFIED` | Not confirmed recently, but no source has successfully reported it absent |
| `STALE` | Past the staleness horizon with no successful confirmation |
| `CLOSED` | Closed on evidence, with the evidence recorded |
| `REOPENED` | Observed present again after being closed |

A job may move to `CLOSED` only on one of three pieces of evidence, each of which is
persisted in `job_verifications` and referenced from `jobs.closed_evidence_id`:

1. `SOURCE_LISTING_ABSENT` - a source responded successfully, the response parsed, it
   contained other postings, and this posting was not among them.
2. `DIRECT_FETCH_NOT_FOUND` - the canonical posting URL returned an authoritative 404 or
   410, or a body the adapter recognizes as a closed-posting page.
3. `DEADLINE_PASSED` - a parsed, non-sentinel deadline is in the past and the posting no
   longer accepts applications.

Explicitly not evidence of closure: a timeout, a connection failure, a 5xx, a 429, an empty
response, a parse failure, or an open circuit breaker. None of these advances anything, and
each leaves every affected job exactly where it was.

Rung 1 additionally requires the listing to be **complete**. An adapter reports
`listingComplete = false` when it truncated at a record cap or gave up on a page, and a
partial listing reconciles nothing: absence from half a listing is not absence.

Where the failure is recorded depends on its scope. A failure to fetch a whole listing is
recorded once, on `source_health` and on the run's counters, because the run obtained no
evidence about any individual posting. A failure while verifying one specific posting is
recorded as a `JobVerification` with outcome `ERROR` against that job, because it is
evidence that the check was attempted and did not conclude. A source that fails to respond
moves its jobs toward `UNVERIFIED` and then `STALE`, which is a statement about the
system's knowledge rather than about the employer.

`first_seen_at` is set once at insert and never written again. `last_seen_at` advances only
on a successful observation. `last_verified_at` advances only on a verification whose
outcome is `PRESENT`. Closure sets `closed_at`; a later `PRESENT` observation sets
`reopened_at`, moves the state to `REOPENED`, and leaves `closed_at` in place as history.

Every transition writes a `job_lifecycle_events` row with the previous state, the new state,
a reason code, the run that caused it and the evidence.

## Consequences

- A provider outage produces a visible gap in verification, not a wave of closures.
- "Why is this job closed" is answerable by joining one row to its evidence snapshot.
- Jobs linger in `UNVERIFIED` and `STALE` longer than a naive implementation would keep
  them, which is the intended trade. The UI sorts and filters on verification recency so
  staleness is visible rather than silently corrected.
- Sentinel deadlines are handled by parsing into a nullable `deadline_at` plus an
  explicit `deadline_open_ended` flag, so a continuous-hiring posting is never closed by
  the deadline rule.
