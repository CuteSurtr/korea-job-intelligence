# ADR 0007 - Scoring separation and explainability

- Status: accepted
- Date: 2026-09-03

## Context

A role that would teach a great deal about production backend engineering may be closed to
a new graduate. A role that a new graduate can certainly get may consist of manual
labeling. Collapsing both into one number makes the two failure modes indistinguishable and
makes the ranking impossible to argue with.

The probes on 2026-09-03 produced clean examples of the split. One employer's board carried
`Staff Backend Engineer (Orchestration Platform)` with Airflow, Kubernetes, Spark, Java and
Terraform, which is excellent engineering experience and closed to a new graduate; the same
search returned an entry-level posting at a company whose stack is a single web framework.
The same board also carried `Barista Support` and `Beta Tester`, which are accessible and
worth zero toward a software engineering career.

## Decision

Three scores, computed and stored separately in `job_scores`, keyed by `score_kind`:

1. **`CAREER_VALUE`** - how much professional software engineering experience the role
   plausibly provides. A property of the job. Independent of any candidate.
2. **`CANDIDATE_FIT`** - how obtainable the role is for one candidate profile, and how well
   it matches their stated direction. Keyed by `profile_id`.
3. **`APPLICATION_PRIORITY`** - a combination of the two with deadline urgency and company
   risk, which is the only one the default sort uses.

Every score row carries `score_version`, `component_scores` as JSON, and a human-readable
`explanation` built from the components that actually moved it. A score without its
components is not written.

Career value components are signed and evidence-backed. Positive weight comes from
production coding, backend and API work, databases, distributed systems, cloud, containers,
CI/CD, observability, data pipelines, automated testing, production ownership and
version-controlled collaboration. Negative weight comes from manual data labeling, content
moderation, generic operations, manual-only QA, sales, clerical work, and roles whose
technical-sounding title is not backed by any development signal in the body.

Each component cites the `job_skills` rows or the `job_intelligence_fields` evidence that
produced it, so a component is traceable to a phrase in a snapshot.

Scores are derived data. They are recomputed from intelligence whenever the extractor or
the score version changes, they are never edited in place, and no pipeline decision depends
on them. Nothing about a job's lifecycle, deduplication or persistence consults a score.

## Consequences

- The two useful questions stay separately answerable: is this good experience, and can I
  realistically get it.
- Reweighting is a version bump and a recompute, and old rows remain to compare against.
- An unexplained number cannot be produced, because the explanation is built from the same
  components that produce the score rather than written alongside it.
- Extraction quality bounds scoring quality. Scores therefore carry the confidence of the
  intelligence they consumed, and a job with thin extraction scores low-confidence rather
  than low.
