# ADR 0004 - PostgreSQL-first search and matching

- Status: accepted
- Date: 2026-09-03

## Context

The system needs full-text search over job descriptions, fuzzy matching over company names
whose renderings differ by a single character, and faceted filtering across roughly twenty
fields. The expected corpus is thousands to low tens of thousands of active postings.

Company name matching is the sharper problem. The same employer came back from two Korean
boards as two strings that differ only in which parenthesis characters were used, and the
same employer is also known by a short brand name and by a romanized name. Exact matching
fails on all three axes.

## Decision

PostgreSQL only, with three mechanisms:

1. **Full-text search.** A `tsvector` column on `jobs` maintained from title, company name
   and description, with a GIN index. Korean is not covered by a bundled dictionary, so the
   `simple` configuration is used and the normalizer contributes tokenized terms rather
   than relying on stemming.
2. **`pg_trgm`.** GIN trigram indexes on `companies.normalized_name`,
   `company_aliases.normalized_alias` and `jobs.normalized_title`. Trigram similarity is
   what actually solves the parenthesis and romanization problem, and it is a `similarity()`
   call rather than a service.
3. **Deterministic normalization before either.** Legal-form prefixes and suffixes, width
   variants, punctuation and whitespace are stripped into a `normalized_name` at write
   time, so the index sees canonical text and the fuzzy layer only handles what
   normalization legitimately cannot.

Redis caches search results and expensive aggregates. It is never authoritative: every
cached response is reproducible from PostgreSQL, and the application serves correctly with
Redis down.

`pgvector` is not adopted. It would be adopted only against a concrete failure: a measured
set of duplicate pairs that trigram similarity and the deterministic ladder in ADR 0006
both miss, where an embedding demonstrably catches them. Until that set exists, it is a
dependency with no defect to justify it.

Elasticsearch is not adopted. Nothing in the requirements needs a query PostgreSQL cannot
serve at this volume, and it would add a second copy of the truth to keep in sync.

## Consequences

- One datastore to back up, migrate and reason about, and search that participates in the
  same transaction as the write that changed it.
- The revisit trigger is explicit: full-text latency past roughly 300 ms at p95 on the
  filtered job query, or a documented set of duplicates that only semantic similarity
  catches.
- Korean tokenization is weaker than a dedicated analyzer would give. The mitigation is
  that skill and technology matching runs against the extracted `job_skills` rows, which
  are structured, rather than against free text.
