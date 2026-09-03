CREATE TABLE skills (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(64)  NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    category      VARCHAR(32)  NOT NULL,
    aliases       TEXT[]       NOT NULL DEFAULT ARRAY[]::TEXT[],
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_skills_slug UNIQUE (slug),
    CONSTRAINT ck_skills_category CHECK (
        category IN ('LANGUAGE', 'FRAMEWORK', 'CLOUD', 'DATABASE', 'INFRA', 'TOOL', 'PRACTICE', 'DOMAIN')
    )
);

CREATE TABLE job_intelligence (
    id                    BIGSERIAL PRIMARY KEY,
    job_id                BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    extractor_version     VARCHAR(32)  NOT NULL,
    source_snapshot_id    BIGINT       REFERENCES job_snapshots (id) ON DELETE SET NULL,
    role_family           VARCHAR(48),
    seniority_bucket      VARCHAR(1),
    seniority_label       VARCHAR(48),
    years_experience_min  INTEGER,
    years_experience_max  INTEGER,
    degree_required       VARCHAR(48),
    degree_preferred      VARCHAR(48),
    employment_type       VARCHAR(32),
    remote_policy         VARCHAR(24),
    location_country      VARCHAR(2),
    location_region       VARCHAR(120),
    location_city         VARCHAR(120),
    location_raw          TEXT,
    salary_min            BIGINT,
    salary_max            BIGINT,
    salary_currency       VARCHAR(3),
    salary_period         VARCHAR(16),
    responsibilities      TEXT[]       NOT NULL DEFAULT ARRAY[]::TEXT[],
    requirements          TEXT[]       NOT NULL DEFAULT ARRAY[]::TEXT[],
    preferred_requirements TEXT[]      NOT NULL DEFAULT ARRAY[]::TEXT[],
    extracted_at          TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_job_intelligence_job UNIQUE (job_id),
    CONSTRAINT ck_job_intelligence_seniority CHECK (
        seniority_bucket IS NULL OR seniority_bucket IN ('A', 'B', 'C', 'D', 'X')
    ),
    CONSTRAINT ck_job_intelligence_years CHECK (
        years_experience_min IS NULL OR years_experience_max IS NULL
        OR years_experience_max >= years_experience_min
    ),
    CONSTRAINT ck_job_intelligence_remote CHECK (
        remote_policy IS NULL OR remote_policy IN ('ONSITE', 'HYBRID', 'REMOTE', 'UNKNOWN')
    )
);

CREATE INDEX idx_job_intelligence_role_seniority
    ON job_intelligence (role_family, seniority_bucket);
CREATE INDEX idx_job_intelligence_years ON job_intelligence (years_experience_min);

CREATE TABLE job_intelligence_fields (
    id                   BIGSERIAL PRIMARY KEY,
    job_id               BIGINT        NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    field_name           VARCHAR(64)   NOT NULL,
    field_value          TEXT,
    confidence           NUMERIC(4, 3) NOT NULL,
    evidence_text        TEXT,
    evidence_snapshot_id BIGINT        REFERENCES job_snapshots (id) ON DELETE SET NULL,
    evidence_source_id   BIGINT        REFERENCES sources (id),
    extraction_method    VARCHAR(48)   NOT NULL,
    extractor_version    VARCHAR(32)   NOT NULL,
    extracted_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_job_intelligence_fields UNIQUE (job_id, field_name, extractor_version),
    CONSTRAINT ck_job_intelligence_fields_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_job_intelligence_fields_method CHECK (
        extraction_method IN ('SOURCE_STRUCTURED', 'PATTERN_MATCH', 'LEXICON', 'HEURISTIC', 'MANUAL')
    )
);

CREATE INDEX idx_job_intelligence_fields_job ON job_intelligence_fields (job_id);

CREATE TABLE job_skills (
    id                   BIGSERIAL PRIMARY KEY,
    job_id               BIGINT        NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    skill_slug           VARCHAR(64)   NOT NULL REFERENCES skills (slug),
    requirement_level    VARCHAR(16)   NOT NULL,
    confidence           NUMERIC(4, 3) NOT NULL,
    evidence_text        TEXT,
    evidence_snapshot_id BIGINT        REFERENCES job_snapshots (id) ON DELETE SET NULL,
    extractor_version    VARCHAR(32)   NOT NULL,
    CONSTRAINT uk_job_skills UNIQUE (job_id, skill_slug, requirement_level),
    CONSTRAINT ck_job_skills_level CHECK (
        requirement_level IN ('REQUIRED', 'PREFERRED', 'MENTIONED')
    ),
    CONSTRAINT ck_job_skills_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX idx_job_skills_job ON job_skills (job_id);
CREATE INDEX idx_job_skills_slug ON job_skills (skill_slug, requirement_level);

CREATE TABLE candidate_profiles (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(64)  NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    profile      JSONB        NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_candidate_profiles_code UNIQUE (code)
);

CREATE TABLE job_scores (
    id               BIGSERIAL PRIMARY KEY,
    job_id           BIGINT        NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    score_kind       VARCHAR(32)   NOT NULL,
    profile_id       BIGINT        REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    score            NUMERIC(6, 2) NOT NULL,
    score_version    VARCHAR(32)   NOT NULL,
    confidence       NUMERIC(4, 3) NOT NULL DEFAULT 1.000,
    component_scores JSONB         NOT NULL,
    explanation      TEXT          NOT NULL,
    computed_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_job_scores_kind CHECK (
        score_kind IN ('CAREER_VALUE', 'CANDIDATE_FIT', 'APPLICATION_PRIORITY')
    ),
    CONSTRAINT ck_job_scores_profile CHECK (
        (score_kind = 'CAREER_VALUE' AND profile_id IS NULL)
        OR (score_kind <> 'CAREER_VALUE' AND profile_id IS NOT NULL)
    ),
    CONSTRAINT ck_job_scores_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE UNIQUE INDEX uk_job_scores_career
    ON job_scores (job_id, score_kind, score_version)
    WHERE profile_id IS NULL;
CREATE UNIQUE INDEX uk_job_scores_profile
    ON job_scores (job_id, score_kind, score_version, profile_id)
    WHERE profile_id IS NOT NULL;
CREATE INDEX idx_job_scores_kind_value ON job_scores (score_kind, score DESC);
