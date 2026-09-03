CREATE TABLE applications (
    id                    BIGSERIAL PRIMARY KEY,
    job_id                BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    profile_id            BIGINT       NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    status                VARCHAR(24)  NOT NULL DEFAULT 'NOT_REVIEWED',
    applied_at            TIMESTAMPTZ,
    resume_version        VARCHAR(120),
    cover_letter_version  VARCHAR(120),
    contact_name          VARCHAR(160),
    contact_email         VARCHAR(255),
    referral              VARCHAR(255),
    interview_stage       VARCHAR(120),
    interview_notes       TEXT,
    follow_up_at          TIMESTAMPTZ,
    rejection_at          TIMESTAMPTZ,
    offer_at              TIMESTAMPTZ,
    notes                 TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_applications_job_profile UNIQUE (job_id, profile_id),
    CONSTRAINT ck_applications_status CHECK (
        status IN ('NOT_REVIEWED', 'INTERESTED', 'READY_TO_APPLY', 'APPLIED',
                   'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN', 'IGNORED')
    )
);

CREATE INDEX idx_applications_profile_status ON applications (profile_id, status);
CREATE INDEX idx_applications_follow_up ON applications (follow_up_at)
    WHERE follow_up_at IS NOT NULL;

CREATE TABLE application_status_history (
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    from_status    VARCHAR(24),
    to_status      VARCHAR(24) NOT NULL,
    changed_at     TIMESTAMPTZ NOT NULL,
    note           TEXT
);

CREATE INDEX idx_application_status_history_application
    ON application_status_history (application_id, changed_at DESC);
