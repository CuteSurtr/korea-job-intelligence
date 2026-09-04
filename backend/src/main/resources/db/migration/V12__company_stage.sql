-- How large an employer is, which is what separates a hidden gem from a job everyone can see.
--
-- The existing company_type column has never been written by any code path, and its meaning is
-- unstated. Rather than overload it, this records the one distinction the product turns on:
-- whether an employer is a large, well-known name, or something a job seeker would not already
-- have on their list.
ALTER TABLE companies
    ADD COLUMN stage text,
    ADD COLUMN stage_evidence text,
    ADD COLUMN stage_assessed_at timestamptz;

ALTER TABLE companies
    ADD CONSTRAINT companies_stage_known
        CHECK (stage IS NULL OR stage IN ('LARGE', 'EMERGING'));

-- Denormalised onto the read model so the job list can exclude large employers without a join.
ALTER TABLE jobs
    ADD COLUMN company_stage text;

CREATE INDEX idx_jobs_company_stage ON jobs (company_stage) WHERE company_stage IS NOT NULL;

COMMENT ON COLUMN companies.stage IS
    'LARGE when the employer matched the known-large lexicon, EMERGING when a posting was found '
    'on the employer''s own ATS board and it did not match, null when neither applies.';
COMMENT ON COLUMN companies.stage_evidence IS
    'What established the stage: the matched term, or the source that carried the posting.';
