CREATE TABLE sources (
    id                       BIGSERIAL PRIMARY KEY,
    code                     VARCHAR(64)  NOT NULL,
    display_name             VARCHAR(160) NOT NULL,
    adapter_kind             VARCHAR(32)  NOT NULL,
    runtime_available        BOOLEAN      NOT NULL DEFAULT false,
    trust_tier               SMALLINT     NOT NULL DEFAULT 3,
    stable_external_id       BOOLEAN      NOT NULL DEFAULT true,
    provides_full_description BOOLEAN     NOT NULL DEFAULT false,
    enabled                  BOOLEAN      NOT NULL DEFAULT true,
    base_url                 TEXT,
    config                   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_sources_code UNIQUE (code),
    CONSTRAINT ck_sources_adapter_kind CHECK (
        adapter_kind IN ('DIRECT_API', 'ATS', 'IMPORT', 'CRAWLER', 'MANUAL')
    ),
    CONSTRAINT ck_sources_trust_tier CHECK (trust_tier BETWEEN 1 AND 4)
);

CREATE TABLE source_health (
    id                    BIGSERIAL PRIMARY KEY,
    source_id             BIGINT      NOT NULL REFERENCES sources (id) ON DELETE CASCADE,
    last_success_at       TIMESTAMPTZ,
    last_failure_at       TIMESTAMPTZ,
    last_attempt_at       TIMESTAMPTZ,
    last_status           VARCHAR(32),
    last_http_status      INTEGER,
    last_error            TEXT,
    last_latency_ms       INTEGER,
    rolling_latency_ms    INTEGER,
    records_last_run      INTEGER      NOT NULL DEFAULT 0,
    consecutive_failures  INTEGER      NOT NULL DEFAULT 0,
    total_successes       BIGINT       NOT NULL DEFAULT 0,
    total_failures        BIGINT       NOT NULL DEFAULT 0,
    rate_limit_events     BIGINT       NOT NULL DEFAULT 0,
    rate_limited_until    TIMESTAMPTZ,
    circuit_state         VARCHAR(16)  NOT NULL DEFAULT 'CLOSED',
    circuit_opened_at     TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_source_health_source UNIQUE (source_id),
    CONSTRAINT ck_source_health_circuit CHECK (
        circuit_state IN ('CLOSED', 'OPEN', 'HALF_OPEN')
    )
);
