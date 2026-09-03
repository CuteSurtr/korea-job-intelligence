CREATE TABLE search_runs (
    id                   BIGSERIAL PRIMARY KEY,
    run_uuid             UUID         NOT NULL,
    source_id            BIGINT       NOT NULL REFERENCES sources (id),
    trigger_kind         VARCHAR(24)  NOT NULL,
    query_text           TEXT,
    query_params         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status               VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
    started_at           TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ,
    duration_ms          BIGINT,
    records_received     INTEGER      NOT NULL DEFAULT 0,
    records_normalized   INTEGER      NOT NULL DEFAULT 0,
    new_jobs             INTEGER      NOT NULL DEFAULT 0,
    updated_jobs         INTEGER      NOT NULL DEFAULT 0,
    duplicates           INTEGER      NOT NULL DEFAULT 0,
    failures             INTEGER      NOT NULL DEFAULT 0,
    rate_limit_events    INTEGER      NOT NULL DEFAULT 0,
    jobs_closed          INTEGER      NOT NULL DEFAULT 0,
    error_summary        TEXT,
    collector            VARCHAR(120),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_search_runs_uuid UNIQUE (run_uuid),
    CONSTRAINT ck_search_runs_trigger CHECK (
        trigger_kind IN ('SCHEDULED', 'MANUAL', 'IMPORT', 'VERIFICATION', 'BACKFILL')
    ),
    CONSTRAINT ck_search_runs_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED', 'SKIPPED')
    )
);

CREATE INDEX idx_search_runs_source_started ON search_runs (source_id, started_at DESC);
CREATE INDEX idx_search_runs_started ON search_runs (started_at DESC);

CREATE TABLE ingestion_failures (
    id             BIGSERIAL PRIMARY KEY,
    search_run_id  BIGINT       NOT NULL REFERENCES search_runs (id) ON DELETE CASCADE,
    source_id      BIGINT       NOT NULL REFERENCES sources (id),
    stage          VARCHAR(32)  NOT NULL,
    reason_code    VARCHAR(64)  NOT NULL,
    message        TEXT         NOT NULL,
    external_id    VARCHAR(320),
    source_url     TEXT,
    raw_payload    JSONB,
    raw_line       TEXT,
    payload_hash   VARCHAR(64),
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_ingestion_failures_stage CHECK (
        stage IN ('FETCH', 'PARSE', 'NORMALIZE', 'COMPANY_RESOLUTION', 'JOB_RESOLUTION',
                  'DEDUPLICATION', 'INTELLIGENCE', 'VERIFICATION', 'PERSIST', 'INDEX', 'RANK')
    )
);

CREATE INDEX idx_ingestion_failures_run ON ingestion_failures (search_run_id);
CREATE INDEX idx_ingestion_failures_source_time
    ON ingestion_failures (source_id, occurred_at DESC);

CREATE TABLE job_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    source_id           BIGINT       NOT NULL REFERENCES sources (id),
    search_run_id       BIGINT       REFERENCES search_runs (id),
    external_id         VARCHAR(320),
    external_key        VARCHAR(320) NOT NULL,
    source_url          TEXT,
    original_apply_url  TEXT,
    fetched_at          TIMESTAMPTZ  NOT NULL,
    raw_title           TEXT,
    raw_company         TEXT,
    raw_location        TEXT,
    raw_description     TEXT,
    raw_requirements    TEXT,
    raw_employment_type TEXT,
    raw_experience      TEXT,
    raw_education       TEXT,
    raw_deadline        TEXT,
    raw_payload         JSONB        NOT NULL,
    payload_hash        VARCHAR(64)     NOT NULL,
    content_hash        VARCHAR(64)     NOT NULL,
    job_id              BIGINT,
    job_source_id       BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_job_snapshots_content UNIQUE (source_id, external_key, content_hash)
);

CREATE INDEX idx_job_snapshots_job ON job_snapshots (job_id, fetched_at DESC);
CREATE INDEX idx_job_snapshots_source_key ON job_snapshots (source_id, external_key);
CREATE INDEX idx_job_snapshots_run ON job_snapshots (search_run_id);
