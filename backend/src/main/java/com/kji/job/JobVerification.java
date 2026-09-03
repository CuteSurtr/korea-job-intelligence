package com.kji.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_verifications")
public class JobVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_source_id")
    private Long jobSourceId;

    @Column(name = "search_run_id")
    private Long searchRunId;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private Method method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Outcome outcome;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "snapshot_id")
    private Long snapshotId;

    private String detail;

    protected JobVerification() {
    }

    public JobVerification(Long jobId, Long jobSourceId, Long searchRunId, Instant verifiedAt,
                           Method method, Outcome outcome, Integer httpStatus,
                           Long snapshotId, String detail) {
        this.jobId = jobId;
        this.jobSourceId = jobSourceId;
        this.searchRunId = searchRunId;
        this.verifiedAt = verifiedAt;
        this.method = method;
        this.outcome = outcome;
        this.httpStatus = httpStatus;
        this.snapshotId = snapshotId;
        this.detail = detail;
    }

    public boolean supportsClosure() {
        return outcome == Outcome.ABSENT
                && (method == Method.SOURCE_LISTING_ABSENT
                || method == Method.DIRECT_FETCH_NOT_FOUND
                || method == Method.DEADLINE_PASSED);
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

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Method getMethod() {
        return method;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public String getDetail() {
        return detail;
    }

    public enum Method {
        SOURCE_LISTING_PRESENT,
        SOURCE_LISTING_ABSENT,
        DIRECT_FETCH_OK,
        DIRECT_FETCH_NOT_FOUND,
        DEADLINE_PASSED,
        SOURCE_UNAVAILABLE
    }

    public enum Outcome {
        PRESENT,
        ABSENT,
        ERROR,
        INCONCLUSIVE
    }
}
