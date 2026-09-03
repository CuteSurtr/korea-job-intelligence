CREATE TABLE companies (
    id                  BIGSERIAL PRIMARY KEY,
    canonical_name      VARCHAR(320) NOT NULL,
    normalized_name     VARCHAR(320) NOT NULL,
    name_ko             VARCHAR(320),
    name_en             VARCHAR(320),
    website_domain      VARCHAR(255),
    country_code        CHAR(2),
    industry            VARCHAR(160),
    company_type        VARCHAR(48),
    founded_on          DATE,
    employee_count      INTEGER,
    risk_level          VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    risk_assessed_at    TIMESTAMPTZ,
    risk_score_version  VARCHAR(32),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_companies_normalized_name UNIQUE (normalized_name),
    CONSTRAINT ck_companies_risk_level CHECK (
        risk_level IN ('LOW', 'MODERATE', 'HIGH', 'UNKNOWN')
    )
);

CREATE INDEX idx_companies_normalized_name_trgm
    ON companies USING gin (normalized_name gin_trgm_ops);
CREATE INDEX idx_companies_website_domain
    ON companies (website_domain) WHERE website_domain IS NOT NULL;

CREATE TABLE company_aliases (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    alias            VARCHAR(320) NOT NULL,
    normalized_alias VARCHAR(320) NOT NULL,
    alias_kind       VARCHAR(32)  NOT NULL DEFAULT 'OBSERVED',
    source_id        BIGINT       REFERENCES sources (id),
    first_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_company_aliases_normalized UNIQUE (normalized_alias),
    CONSTRAINT ck_company_aliases_kind CHECK (
        alias_kind IN ('LEGAL', 'BRAND', 'ROMANIZED', 'OBSERVED', 'MANUAL')
    )
);

CREATE INDEX idx_company_aliases_company ON company_aliases (company_id);
CREATE INDEX idx_company_aliases_trgm
    ON company_aliases USING gin (normalized_alias gin_trgm_ops);

CREATE TABLE company_identifiers (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    identifier_type  VARCHAR(48)  NOT NULL,
    identifier_value VARCHAR(320) NOT NULL,
    source_id        BIGINT       REFERENCES sources (id),
    observed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_company_identifiers UNIQUE (identifier_type, identifier_value)
);

CREATE INDEX idx_company_identifiers_company ON company_identifiers (company_id);

CREATE TABLE company_metrics (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    metric_key       VARCHAR(64)  NOT NULL,
    numeric_value    NUMERIC(20, 4),
    text_value       TEXT,
    unit             VARCHAR(32),
    effective_date   DATE,
    observed_at      TIMESTAMPTZ  NOT NULL,
    source_id        BIGINT       REFERENCES sources (id),
    evidence_url     TEXT,
    confidence       NUMERIC(4, 3),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_company_metrics_value CHECK (
        numeric_value IS NOT NULL OR text_value IS NOT NULL
    ),
    CONSTRAINT ck_company_metrics_confidence CHECK (
        confidence IS NULL OR (confidence >= 0 AND confidence <= 1)
    )
);

CREATE INDEX idx_company_metrics_lookup
    ON company_metrics (company_id, metric_key, observed_at DESC);

CREATE TABLE company_risk_reasons (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT      NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    assessed_at   TIMESTAMPTZ NOT NULL,
    risk_level    VARCHAR(16) NOT NULL,
    reason_code   VARCHAR(64) NOT NULL,
    reason_detail TEXT        NOT NULL,
    evidence      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_company_risk_reasons_level CHECK (
        risk_level IN ('LOW', 'MODERATE', 'HIGH', 'UNKNOWN')
    )
);

CREATE INDEX idx_company_risk_reasons_company
    ON company_risk_reasons (company_id, assessed_at DESC);
