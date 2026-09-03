ALTER TABLE jobs
    ADD COLUMN role_family               VARCHAR(48),
    ADD COLUMN seniority_bucket          VARCHAR(1),
    ADD COLUMN years_experience_min      INTEGER,
    ADD COLUMN years_experience_max      INTEGER,
    ADD COLUMN remote_policy             VARCHAR(24),
    ADD COLUMN employment_type           VARCHAR(32),
    ADD COLUMN degree_required           VARCHAR(48),
    ADD COLUMN career_value_score        NUMERIC(6, 2),
    ADD COLUMN candidate_fit_score       NUMERIC(6, 2),
    ADD COLUMN application_priority_score NUMERIC(6, 2),
    ADD COLUMN scored_at                 TIMESTAMPTZ;

ALTER TABLE jobs
    ADD CONSTRAINT ck_jobs_seniority_bucket CHECK (
        seniority_bucket IS NULL OR seniority_bucket IN ('A', 'B', 'C', 'D', 'X')
    );

CREATE INDEX idx_jobs_career_value ON jobs (career_value_score DESC NULLS LAST);
CREATE INDEX idx_jobs_priority ON jobs (application_priority_score DESC NULLS LAST);
CREATE INDEX idx_jobs_role_seniority ON jobs (role_family, seniority_bucket);
CREATE INDEX idx_jobs_years_min ON jobs (years_experience_min);
