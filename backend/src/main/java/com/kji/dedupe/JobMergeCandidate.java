package com.kji.dedupe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_merge_candidates")
public class JobMergeCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "left_job_id", nullable = false)
    private Long leftJobId;

    @Column(name = "right_job_id", nullable = false)
    private Long rightJobId;

    @Column(name = "match_method", nullable = false, length = 48)
    private String matchMethod;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidence = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_note")
    private String resolvedNote;

    protected JobMergeCandidate() {
    }

    public static JobMergeCandidate between(Long firstJobId, Long secondJobId, String matchMethod,
                                            BigDecimal confidence, String evidence,
                                            Instant detectedAt) {
        JobMergeCandidate candidate = new JobMergeCandidate();
        candidate.leftJobId = Math.min(firstJobId, secondJobId);
        candidate.rightJobId = Math.max(firstJobId, secondJobId);
        candidate.matchMethod = matchMethod;
        candidate.confidence = confidence;
        candidate.evidence = evidence == null ? "{}" : evidence;
        candidate.status = Status.PENDING;
        candidate.detectedAt = detectedAt;
        return candidate;
    }

    public void resolve(Status status, String note, Instant at) {
        this.status = status;
        this.resolvedNote = note;
        this.resolvedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Long getLeftJobId() {
        return leftJobId;
    }

    public Long getRightJobId() {
        return rightJobId;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolvedNote() {
        return resolvedNote;
    }

    public enum Status {
        PENDING,
        MERGED,
        REJECTED
    }
}
