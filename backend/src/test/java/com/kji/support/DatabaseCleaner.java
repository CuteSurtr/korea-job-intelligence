package com.kji.support;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseCleaner {

    private static final String TRUNCATE = """
            TRUNCATE TABLE job_lifecycle_events, job_verifications, job_sightings,
                           job_merge_candidates, job_snapshots, job_sources, jobs,
                           ingestion_failures, search_runs,
                           company_identifiers, company_aliases, company_metrics,
                           company_risk_reasons, companies
            RESTART IDENTITY CASCADE
            """;

    private final EntityManager entityManager;

    public DatabaseCleaner(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void clean() {
        entityManager.createNativeQuery(TRUNCATE).executeUpdate();
        entityManager.createNativeQuery(
                        "UPDATE source_health SET last_success_at = NULL, last_failure_at = NULL, "
                                + "last_attempt_at = NULL, consecutive_failures = 0, "
                                + "circuit_state = 'CLOSED', circuit_opened_at = NULL, "
                                + "rate_limited_until = NULL, records_last_run = 0")
                .executeUpdate();
    }
}
