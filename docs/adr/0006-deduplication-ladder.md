# ADR 0006 - Deduplication evidence ladder

- Status: accepted
- Date: 2026-09-03

## Context

Merging is destructive in a way splitting is not. Two rows wrongly merged lose one
posting's identity, its deadline and its apply URL, and the loss is invisible afterwards.
Two rows wrongly left apart show up as a duplicate in a list, which a person notices
immediately.

The pressure to merge aggressively comes from titles. A single Korean employer's postings
included several distinct openings whose titles differ by a parenthetical, and a
cross-provider search returned `Backend Engineer (Junior)` and `Backend Engineer (Senior)`
at the same company in the same city, posted the same day. Title similarity between those
two is very high and they are emphatically not the same job.

## Decision

An ordered ladder. The first rung that fires decides, and every decision records the rung,
its confidence and its evidence in `job_sources.match_method`, `match_confidence` and
`match_evidence`.

| Rung | Evidence | Confidence | Auto-merge |
| --- | --- | --- | --- |
| 1 | Identical canonical apply URL after canonicalization | 1.00 | yes |
| 2 | Identical ATS provider and external job id | 0.98 | yes |
| 3 | Same company and same normalized title and compatible location | 0.85 | yes |
| 4 | Same company, high description similarity, compatible location | 0.75 | yes |
| 5 | Semantic similarity | not implemented | no |

Nothing below 0.75 merges automatically. A candidate pair that scores between 0.60 and
0.75 is written to a review queue with both jobs intact, because an unreviewed duplicate is
cheaper than an unreviewable merge.

**Rungs 3 and 4 are cross-source only.** If the job a rung would merge into already carries a
row from the same source the candidate came from, the rung does not fire, and the pair is
written to `job_merge_candidates` with both jobs intact. A provider listing two postings is
that provider asserting two postings; weaker title or description similarity does not
overturn the provider's own distinction.

That rule came out of a live run rather than out of theory. One employer's Greenhouse board
returned 566 postings and rung 3 merged 98 of them, collapsing six separately numbered
`Server Developer` openings into a single job because the company, the normalized title and
the city all matched. Greenhouse had given each of them its own id and its own URL, which is
the employer stating that they are different openings. Rung 3 exists to reconcile one opening
seen through different providers; it has no business overruling a provider about its own
catalogue.

Definitions that carry the weight:

- **Canonicalization** strips tracking parameters, lowercases the host, drops default
  ports, removes trailing slashes and fragments, and preserves the identifying query
  parameters an ATS actually uses. This matters concretely: a provider observed on
  2026-09-03 appends `utm_source` to the employer's own Greenhouse and Ashby URLs, so the
  raw strings differ while the postings are identical.
- **Normalized title** removes bracketed brand prefixes, collapses width variants and
  whitespace, and lowercases, but never removes a seniority or level token. `(Junior)` and
  `(Senior)` survive normalization and keep rung 3 from firing on that pair.
- **Compatible location** means neither side asserts a different city. One side being
  unknown is compatible; two different known cities are not.
- **Description similarity** is trigram similarity over the normalized description, and it
  only applies when both sides have a description of at least 400 characters. A snippet is
  not a description.

Rung 2 requires the provider to have a stable external id. A provider whose identifiers are
per-response rather than durable is marked `stable_external_id = false` in the registry and
is skipped at this rung entirely; one of the probed providers behaves this way.

Every merge is reversible. `job_sources` rows carry the job they were attached to and a
`manually_corrected` flag, so a bad merge is undone by reattaching rows rather than by
recovering lost data.

## Consequences

- The system under-merges rather than over-merges, and the residual duplicates are visible
  in the review queue instead of being invisible in the data.
- Every merge is explainable to the row that caused it.
- Rung 4 costs a trigram comparison against candidates from the same company only, which
  keeps it bounded without an index over the whole corpus.
- Rung 5 is deliberately unimplemented. ADR 0004 records what would have to be true for it
  to earn its dependency.
