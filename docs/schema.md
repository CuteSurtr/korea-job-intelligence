# Database schema

PostgreSQL 16. Flyway migrations live in `backend/src/main/resources/db/migration`.
Enumerations are `VARCHAR` with `CHECK` constraints rather than PostgreSQL enum types, so
adding a value is a migration rather than a type rewrite.

## Migration order

| Migration | Contents |
| --- | --- |
| `V1__extensions.sql` | `pg_trgm`, `btree_gin` |
| `V2__sources.sql` | `sources`, `source_health` |
| `V3__companies.sql` | `companies`, `company_aliases`, `company_identifiers`, `company_metrics`, `company_risk_reasons` |
| `V4__ingestion.sql` | `search_runs`, `ingestion_failures`, `job_snapshots` |
| `V5__jobs.sql` | `jobs`, `job_sources`, `job_sightings`, `job_verifications`, `job_lifecycle_events`, `job_merge_candidates` |
| `V6__intelligence.sql` | `skills`, `job_intelligence`, `job_intelligence_fields`, `job_skills`, `candidate_profiles`, `job_scores` |
| `V7__crm.sql` | `applications`, `application_status_history` |
| `V8__seed_reference_data.sql` | Source registry, skill dictionary, default candidate profile |

## Identity and provenance

`job_snapshots` is the evidence table. Every conclusion the system states about a posting
resolves to a row here. It is written on content change rather than on every observation:
`content_hash` covers the semantic fields, `payload_hash` covers the whole provider
payload, and `uk_job_snapshots_content` on `(source_id, external_key, content_hash)` stops
an unchanged posting from writing a new row on every run.

`external_key` exists because not every provider supplies a durable id. It is the external
id when there is one, and a hash of the canonical URL otherwise, so `job_sources` always has
a unique key per source.

`job_snapshots.job_id` and `job_source_id` are nullable and `ON DELETE SET NULL`. A snapshot
that could not be resolved to a job is still evidence, and deleting a job must not destroy
the record of what was observed.

## Answering "why does the system believe this"

```sql
SELECT f.field_name,
       f.field_value,
       f.confidence,
       f.extraction_method,
       f.evidence_text,
       s.source_url,
       s.fetched_at,
       src.code
FROM job_intelligence_fields f
JOIN job_snapshots s ON s.id = f.evidence_snapshot_id
JOIN sources src     ON src.id = s.source_id
WHERE f.job_id = :jobId
  AND f.field_name = 'years_experience_min';
```

Every extracted field carries the phrase it was read from, the snapshot that phrase came
from, the method that read it and a confidence. A field with no evidence is not written,
which is how "unknown" stays distinguishable from "zero".

## Lifecycle integrity

`jobs.closed_evidence_id` points at the `job_verifications` row that justified closure. The
column is nullable, but application code refuses to set `lifecycle_state = 'CLOSED'` without
it, and the lifecycle tests assert that a source failure produces a verification with
outcome `ERROR` and no state change.

`ck_jobs_seen_order` enforces `last_seen_at >= first_seen_at` at the database level, which
makes an accidental overwrite of `first_seen_at` a constraint violation rather than silent
data loss.

## Deduplication surfaces

`uk_jobs_canonical_url_key` is a partial unique index, so two jobs cannot share a
canonicalized apply URL, and jobs without one are unconstrained. `job_sources` carries
`match_method`, `match_confidence` and `match_evidence` for every attachment, and
`manually_corrected` marks rows a person has moved by hand so a later run does not undo the
correction.

`job_merge_candidates` holds pairs that scored above the review threshold but below the
auto-merge threshold. `ck_job_merge_candidates_order` requires `left_job_id < right_job_id`
so a pair cannot be queued twice in opposite orders.

## Scoring

`job_scores` is keyed by `(job_id, score_kind, score_version)` for career value and
additionally by `profile_id` for the two candidate-specific kinds, enforced by two partial
unique indexes. `ck_job_scores_profile` requires career value to have no profile and the
other kinds to have one, so the separation in ADR 0007 cannot be violated by a write.
`component_scores` and `explanation` are both `NOT NULL`: a score without its components
cannot be stored.

## Search

`jobs.search_document` is a `tsvector` with a GIN index, maintained by the application from
title, company name and description. Trigram GIN indexes sit on
`companies.normalized_name`, `company_aliases.normalized_alias` and `jobs.normalized_title`
to support fuzzy company resolution and near-duplicate title detection.
