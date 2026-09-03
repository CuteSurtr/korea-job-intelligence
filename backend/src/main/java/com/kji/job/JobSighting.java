package com.kji.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_sightings")
public class JobSighting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_source_id", nullable = false)
    private Long jobSourceId;

    @Column(name = "search_run_id")
    private Long searchRunId;

    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;

    @Column(name = "content_changed", nullable = false)
    private boolean contentChanged;

    protected JobSighting() {
    }

    public JobSighting(Long jobId, Long jobSourceId, Long searchRunId, Long snapshotId,
                       Instant seenAt, boolean contentChanged) {
        this.jobId = jobId;
        this.jobSourceId = jobSourceId;
        this.searchRunId = searchRunId;
        this.snapshotId = snapshotId;
        this.seenAt = seenAt;
        this.contentChanged = contentChanged;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getJobSourceId() {
        return jobSourceId;
    }

    public Long getSearchRunId() {
        return searchRunId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Instant getSeenAt() {
        return seenAt;
    }

    public boolean isContentChanged() {
        return contentChanged;
    }
}
