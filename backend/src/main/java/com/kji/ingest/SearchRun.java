package com.kji.ingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "search_runs")
public class SearchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_uuid", nullable = false, unique = true)
    private UUID runUuid;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false, length = 24)
    private TriggerKind triggerKind;

    @Column(name = "query_text")
    private String queryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_params", nullable = false, columnDefinition = "jsonb")
    private String queryParams = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "records_received", nullable = false)
    private int recordsReceived;

    @Column(name = "records_normalized", nullable = false)
    private int recordsNormalized;

    @Column(name = "new_jobs", nullable = false)
    private int newJobs;

    @Column(name = "updated_jobs", nullable = false)
    private int updatedJobs;

    @Column(nullable = false)
    private int duplicates;

    @Column(nullable = false)
    private int failures;

    @Column(name = "rate_limit_events", nullable = false)
    private int rateLimitEvents;

    @Column(name = "jobs_closed", nullable = false)
    private int jobsClosed;

    @Column(name = "error_summary")
    private String errorSummary;

    @Column(length = 120)
    private String collector;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SearchRun() {
    }

    public SearchRun(Long sourceId, TriggerKind triggerKind, String queryText,
                     String queryParams, Instant startedAt, String collector) {
        this.runUuid = UUID.randomUUID();
        this.sourceId = sourceId;
        this.triggerKind = triggerKind;
        this.queryText = queryText;
        this.queryParams = queryParams == null ? "{}" : queryParams;
        this.startedAt = startedAt;
        this.collector = collector;
        this.status = Status.RUNNING;
    }

    public void complete(Status status, Instant completedAt, String errorSummary) {
        this.status = status;
        this.completedAt = completedAt;
        this.durationMs = Duration.between(startedAt, completedAt).toMillis();
        this.errorSummary = errorSummary;
    }

    public void addCounters(int received, int normalized, int created, int updated,
                            int duplicates, int failures, int rateLimitEvents) {
        this.recordsReceived += received;
        this.recordsNormalized += normalized;
        this.newJobs += created;
        this.updatedJobs += updated;
        this.duplicates += duplicates;
        this.failures += failures;
        this.rateLimitEvents += rateLimitEvents;
    }

    public void addJobsClosed(int closed) {
        this.jobsClosed += closed;
    }

    public Long getId() {
        return id;
    }

    public UUID getRunUuid() {
        return runUuid;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public TriggerKind getTriggerKind() {
        return triggerKind;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public int getRecordsReceived() {
        return recordsReceived;
    }

    public int getRecordsNormalized() {
        return recordsNormalized;
    }

    public int getNewJobs() {
        return newJobs;
    }

    public int getUpdatedJobs() {
        return updatedJobs;
    }

    public int getDuplicates() {
        return duplicates;
    }

    public int getFailures() {
        return failures;
    }

    public int getRateLimitEvents() {
        return rateLimitEvents;
    }

    public int getJobsClosed() {
        return jobsClosed;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public String getCollector() {
        return collector;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum TriggerKind {
        SCHEDULED,
        MANUAL,
        IMPORT,
        VERIFICATION,
        BACKFILL
    }

    public enum Status {
        RUNNING,
        SUCCEEDED,
        PARTIAL,
        FAILED,
        SKIPPED
    }
}
