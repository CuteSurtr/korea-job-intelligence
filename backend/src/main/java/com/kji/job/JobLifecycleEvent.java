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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_lifecycle_events")
public class JobLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 16)
    private LifecycleState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 16)
    private LifecycleState toState;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "search_run_id")
    private Long searchRunId;

    @Column(name = "verification_id")
    private Long verificationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidence = "{}";

    protected JobLifecycleEvent() {
    }

    public JobLifecycleEvent(Long jobId, LifecycleState fromState, LifecycleState toState,
                             String reasonCode, Instant occurredAt, Long searchRunId,
                             Long verificationId, String evidence) {
        this.jobId = jobId;
        this.fromState = fromState;
        this.toState = toState;
        this.reasonCode = reasonCode;
        this.occurredAt = occurredAt;
        this.searchRunId = searchRunId;
        this.verificationId = verificationId;
        this.evidence = evidence == null ? "{}" : evidence;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public LifecycleState getFromState() {
        return fromState;
    }

    public LifecycleState getToState() {
        return toState;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getSearchRunId() {
        return searchRunId;
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public String getEvidence() {
        return evidence;
    }
}
