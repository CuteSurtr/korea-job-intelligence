package com.kji.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_sources")
public class JobSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "external_id", length = 320)
    private String externalId;

    @Column(name = "external_key", nullable = false, length = 320)
    private String externalKey;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "canonical_url_key", length = 600)
    private String canonicalUrlKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "latest_snapshot_id")
    private Long latestSnapshotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", nullable = false, length = 48)
    private MatchMethod matchMethod;

    @Column(name = "match_confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal matchConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_evidence", nullable = false, columnDefinition = "jsonb")
    private String matchEvidence = "{}";

    @Column(name = "manually_corrected", nullable = false)
    private boolean manuallyCorrected;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobSource() {
    }

    public JobSource(Job job, Long sourceId, String externalId, String externalKey,
                     String sourceUrl, String applyUrl, String canonicalUrlKey,
                     Instant seenAt, MatchMethod matchMethod, BigDecimal matchConfidence,
                     String matchEvidence) {
        this.job = job;
        this.sourceId = sourceId;
        this.externalId = externalId;
        this.externalKey = externalKey;
        this.sourceUrl = sourceUrl;
        this.applyUrl = applyUrl;
        this.canonicalUrlKey = canonicalUrlKey;
        this.firstSeenAt = seenAt;
        this.lastSeenAt = seenAt;
        this.matchMethod = matchMethod;
        this.matchConfidence = matchConfidence;
        this.matchEvidence = matchEvidence == null ? "{}" : matchEvidence;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void observe(Instant seenAt, String sourceUrl, String applyUrl,
                        String canonicalUrlKey, Long latestSnapshotId) {
        if (seenAt.isAfter(lastSeenAt)) {
            lastSeenAt = seenAt;
        }
        if (sourceUrl != null) {
            this.sourceUrl = sourceUrl;
        }
        if (applyUrl != null) {
            this.applyUrl = applyUrl;
        }
        if (canonicalUrlKey != null) {
            this.canonicalUrlKey = canonicalUrlKey;
        }
        if (latestSnapshotId != null) {
            this.latestSnapshotId = latestSnapshotId;
        }
        this.active = true;
        this.closedAt = null;
    }

    public void markVerifiedPresent(Instant verifiedAt) {
        this.lastVerifiedAt = verifiedAt;
        this.active = true;
    }

    public void markAbsent(Instant at) {
        this.active = false;
        this.closedAt = at;
        this.lastVerifiedAt = at;
    }

    public void reassignTo(Job job, MatchMethod matchMethod, BigDecimal matchConfidence,
                           String matchEvidence, boolean manual) {
        this.job = job;
        this.matchMethod = matchMethod;
        this.matchConfidence = matchConfidence;
        this.matchEvidence = matchEvidence == null ? "{}" : matchEvidence;
        this.manuallyCorrected = manual;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public String getCanonicalUrlKey() {
        return canonicalUrlKey;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Long getLatestSnapshotId() {
        return latestSnapshotId;
    }

    public MatchMethod getMatchMethod() {
        return matchMethod;
    }

    public BigDecimal getMatchConfidence() {
        return matchConfidence;
    }

    public String getMatchEvidence() {
        return matchEvidence;
    }

    public boolean isManuallyCorrected() {
        return manuallyCorrected;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum MatchMethod {
        NEW_JOB,
        CANONICAL_URL,
        ATS_EXTERNAL_ID,
        COMPANY_TITLE_LOCATION,
        DESCRIPTION_SIMILARITY,
        SEMANTIC_SIMILARITY,
        MANUAL
    }
}
