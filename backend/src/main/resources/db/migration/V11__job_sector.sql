-- The sector a posting belongs to, which is orthogonal to what the engineer does.
--
-- role_family already says whether a role is backend or frontend. It cannot say whether that
-- backend role is at a bank or a game studio, and "show me financial engineering roles" is a
-- question about the second axis, not the first. Recording it beside the other extracted
-- fields keeps it under the same provenance rules: a value here is evidence-backed or null.
ALTER TABLE job_intelligence
    ADD COLUMN sector text,
    ADD COLUMN sector_confidence numeric(4, 3);

ALTER TABLE job_intelligence
    ADD CONSTRAINT job_intelligence_sector_confidence_range
        CHECK (sector_confidence IS NULL OR (sector_confidence >= 0 AND sector_confidence <= 1));

-- The read model the job list is served from carries the same value, so filtering by sector
-- does not have to join back to the intelligence table.
ALTER TABLE jobs
    ADD COLUMN sector text;

CREATE INDEX idx_jobs_sector ON jobs (sector) WHERE sector IS NOT NULL;

COMMENT ON COLUMN job_intelligence.sector IS
    'Industry the employer operates in, extracted with evidence. Null when unestablished.';
COMMENT ON COLUMN jobs.sector IS
    'Denormalised from job_intelligence.sector for filtering the job list.';
