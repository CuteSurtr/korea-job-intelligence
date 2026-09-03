CREATE TABLE jobs (
    id                       BIGSERIAL PRIMARY KEY,
    company_id               BIGINT       NOT NULL REFERENCES companies (id),
    canonical_title          VARCHAR(500) NOT NULL,
    normalized_title         VARCHAR(500) NOT NULL,
    canonical_apply_url      TEXT,
    canonical_url_key        VARCHAR(600),
    lifecycle_state          VARCHAR(16)  NOT NULL DEFAULT 'DISCOVERED',
    first_seen_at            TIMESTAMPTZ  NOT NULL,
    last_seen_at             TIMESTAMPTZ  NOT NULL,
    last_verified_at         TIMESTAMPTZ,
    closed_at                TIMESTAMPTZ,
    reopened_at              TIMESTAMPTZ,
    closed_reason            VARCHAR(64),
    closed_evidence_id       BIGINT,
    posted_at                TIMESTAMPTZ,
    deadline_at              TIMESTAMPTZ,
    deadline_open_ended      BOOLEAN      NOT NULL DEFAULT false,
    primary_source_id        BIGINT       REFERENCES sources (id),
    source_count             INTEGER      NOT NULL DEFAULT 0,
    search_document          tsvector,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_jobs_lifecycle CHECK (
        lifecycle_state IN ('DISCOVERED', 'ACTIVE', 'UNVERIFIED', 'STALE', 'CLOSED', 'REOPENED')
    ),
    CONSTRAINT ck_jobs_seen_order CHECK (last_seen_at >= first_seen_at)
);

CREATE UNIQUE INDEX uk_jobs_canonical_url_key
    ON jobs (canonical_url_key) WHERE canonical_url_key IS NOT NULL;
CREATE INDEX idx_jobs_company ON jobs (company_id);
CREATE INDEX idx_jobs_lifecycle_seen ON jobs (lifecycle_state, last_seen_at DESC);
CREATE INDEX idx_jobs_first_seen ON jobs (first_seen_at DESC);
CREATE INDEX idx_jobs_deadline ON jobs (deadline_at) WHERE deadline_at IS NOT NULL;
CREATE INDEX idx_jobs_normalized_title_trgm
    ON jobs USING gin (normalized_title gin_trgm_ops);
CREATE INDEX idx_jobs_search_document ON jobs USING gin (search_document);

CREATE TABLE job_sources (
    id                  BIGSERIAL PRIMARY KEY,
    job_id              BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    source_id           BIGINT       NOT NULL REFERENCES sources (id),
    external_id         VARCHAR(320),
    external_key        VARCHAR(320) NOT NULL,
    source_url          TEXT,
    apply_url           TEXT,
    canonical_url_key   VARCHAR(600),
    active              BOOLEAN      NOT NULL DEFAULT true,
    first_seen_at       TIMESTAMPTZ  NOT NULL,
    last_seen_at        TIMESTAMPTZ  NOT NULL,
    last_verified_at    TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    latest_snapshot_id  BIGINT       REFERENCES job_snapshots (id),
    match_method        VARCHAR(48)  NOT NULL,
    match_confidence    NUMERIC(4, 3) NOT NULL,
    match_evidence      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    manually_corrected  BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_job_sources_source_key UNIQUE (source_id, external_key),
    CONSTRAINT ck_job_sources_confidence CHECK (match_confidence >= 0 AND match_confidence <= 1),
    CONSTRAINT ck_job_sources_match_method CHECK (
        match_method IN ('NEW_JOB', 'CANONICAL_URL', 'ATS_EXTERNAL_ID',
                         'COMPANY_TITLE_LOCATION', 'DESCRIPTION_SIMILARITY',
                         'SEMANTIC_SIMILARITY', 'MANUAL')
    )
);

CREATE INDEX idx_job_sources_job ON job_sources (job_id);
CREATE INDEX idx_job_sources_source_active ON job_sources (source_id, active);
CREATE INDEX idx_job_sources_url_key
    ON job_sources (canonical_url_key) WHERE canonical_url_key IS NOT NULL;

ALTER TABLE job_snapshots
    ADD CONSTRAINT fk_job_snapshots_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_job_snapshots_job_source FOREIGN KEY (job_source_id) REFERENCES job_sources (id) ON DELETE SET NULL;

CREATE TABLE job_sightings (
    id             BIGSERIAL PRIMARY KEY,
    job_id         BIGINT      NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    job_source_id  BIGINT      NOT NULL REFERENCES job_sources (id) ON DELETE CASCADE,
    search_run_id  BIGINT      REFERENCES search_runs (id) ON DELETE SET NULL,
    snapshot_id    BIGINT      REFERENCES job_snapshots (id) ON DELETE SET NULL,
    seen_at        TIMESTAMPTZ NOT NULL,
    content_changed BOOLEAN    NOT NULL DEFAULT false
);

CREATE INDEX idx_job_sightings_job_seen ON job_sightings (job_id, seen_at DESC);
CREATE INDEX idx_job_sightings_job_source_seen ON job_sightings (job_source_id, seen_at DESC);
CREATE INDEX idx_job_sightings_run ON job_sightings (search_run_id);

CREATE TABLE job_verifications (
    id             BIGSERIAL PRIMARY KEY,
    job_id         BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    job_source_id  BIGINT       REFERENCES job_sources (id) ON DELETE SET NULL,
    search_run_id  BIGINT       REFERENCES search_runs (id) ON DELETE SET NULL,
    verified_at    TIMESTAMPTZ  NOT NULL,
    method         VARCHAR(48)  NOT NULL,
    outcome        VARCHAR(16)  NOT NULL,
    http_status    INTEGER,
    snapshot_id    BIGINT       REFERENCES job_snapshots (id) ON DELETE SET NULL,
    detail         TEXT,
    CONSTRAINT ck_job_verifications_method CHECK (
        method IN ('SOURCE_LISTING_PRESENT', 'SOURCE_LISTING_ABSENT', 'DIRECT_FETCH_OK',
                   'DIRECT_FETCH_NOT_FOUND', 'DEADLINE_PASSED', 'SOURCE_UNAVAILABLE')
    ),
    CONSTRAINT ck_job_verifications_outcome CHECK (
        outcome IN ('PRESENT', 'ABSENT', 'ERROR', 'INCONCLUSIVE')
    )
);

CREATE INDEX idx_job_verifications_job ON job_verifications (job_id, verified_at DESC);

ALTER TABLE jobs
    ADD CONSTRAINT fk_jobs_closed_evidence
        FOREIGN KEY (closed_evidence_id) REFERENCES job_verifications (id) ON DELETE SET NULL;

CREATE TABLE job_lifecycle_events (
    id            BIGSERIAL PRIMARY KEY,
    job_id        BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    from_state    VARCHAR(16),
    to_state      VARCHAR(16)  NOT NULL,
    reason_code   VARCHAR(64)  NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    search_run_id BIGINT       REFERENCES search_runs (id) ON DELETE SET NULL,
    verification_id BIGINT     REFERENCES job_verifications (id) ON DELETE SET NULL,
    evidence      JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_job_lifecycle_events_job ON job_lifecycle_events (job_id, occurred_at DESC);

CREATE TABLE job_merge_candidates (
    id             BIGSERIAL PRIMARY KEY,
    left_job_id    BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    right_job_id   BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    match_method   VARCHAR(48)  NOT NULL,
    confidence     NUMERIC(4, 3) NOT NULL,
    evidence       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    detected_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at    TIMESTAMPTZ,
    resolved_note  TEXT,
    CONSTRAINT uk_job_merge_candidates UNIQUE (left_job_id, right_job_id),
    CONSTRAINT ck_job_merge_candidates_order CHECK (left_job_id < right_job_id),
    CONSTRAINT ck_job_merge_candidates_status CHECK (
        status IN ('PENDING', 'MERGED', 'REJECTED')
    )
);

CREATE INDEX idx_job_merge_candidates_status ON job_merge_candidates (status, confidence DESC);
