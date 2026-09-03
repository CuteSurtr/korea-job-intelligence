ALTER TABLE jobs
    ADD COLUMN location_raw     TEXT,
    ADD COLUMN location_city    VARCHAR(120),
    ADD COLUMN location_region  VARCHAR(120),
    ADD COLUMN location_country VARCHAR(2);

CREATE INDEX idx_jobs_location_city ON jobs (location_city) WHERE location_city IS NOT NULL;

ALTER TABLE jobs
    ADD COLUMN description TEXT,
    ADD COLUMN normalized_description TEXT;

CREATE INDEX idx_jobs_normalized_description_trgm
    ON jobs USING gin (normalized_description gin_trgm_ops);
