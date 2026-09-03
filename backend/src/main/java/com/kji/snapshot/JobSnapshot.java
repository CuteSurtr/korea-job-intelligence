package com.kji.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_snapshots")
public class JobSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "search_run_id")
    private Long searchRunId;

    @Column(name = "external_id", length = 320)
    private String externalId;

    @Column(name = "external_key", nullable = false, length = 320)
    private String externalKey;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "original_apply_url")
    private String originalApplyUrl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "raw_title")
    private String rawTitle;

    @Column(name = "raw_company")
    private String rawCompany;

    @Column(name = "raw_location")
    private String rawLocation;

    @Column(name = "raw_description")
    private String rawDescription;

    @Column(name = "raw_requirements")
    private String rawRequirements;

    @Column(name = "raw_employment_type")
    private String rawEmploymentType;

    @Column(name = "raw_experience")
    private String rawExperience;

    @Column(name = "raw_education")
    private String rawEducation;

    @Column(name = "raw_deadline")
    private String rawDeadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_source_id")
    private Long jobSourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected JobSnapshot() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public void attachTo(Long jobId, Long jobSourceId) {
        this.jobId = jobId;
        this.jobSourceId = jobSourceId;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getSearchRunId() {
        return searchRunId;
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

    public String getOriginalApplyUrl() {
        return originalApplyUrl;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getRawTitle() {
        return rawTitle;
    }

    public String getRawCompany() {
        return rawCompany;
    }

    public String getRawLocation() {
        return rawLocation;
    }

    public String getRawDescription() {
        return rawDescription;
    }

    public String getRawRequirements() {
        return rawRequirements;
    }

    public String getRawEmploymentType() {
        return rawEmploymentType;
    }

    public String getRawExperience() {
        return rawExperience;
    }

    public String getRawEducation() {
        return rawEducation;
    }

    public String getRawDeadline() {
        return rawDeadline;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getJobSourceId() {
        return jobSourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {

        private final JobSnapshot snapshot = new JobSnapshot();

        public Builder sourceId(Long value) {
            snapshot.sourceId = value;
            return this;
        }

        public Builder searchRunId(Long value) {
            snapshot.searchRunId = value;
            return this;
        }

        public Builder externalId(String value) {
            snapshot.externalId = value;
            return this;
        }

        public Builder externalKey(String value) {
            snapshot.externalKey = value;
            return this;
        }

        public Builder sourceUrl(String value) {
            snapshot.sourceUrl = value;
            return this;
        }

        public Builder originalApplyUrl(String value) {
            snapshot.originalApplyUrl = value;
            return this;
        }

        public Builder fetchedAt(Instant value) {
            snapshot.fetchedAt = value;
            return this;
        }

        public Builder rawTitle(String value) {
            snapshot.rawTitle = value;
            return this;
        }

        public Builder rawCompany(String value) {
            snapshot.rawCompany = value;
            return this;
        }

        public Builder rawLocation(String value) {
            snapshot.rawLocation = value;
            return this;
        }

        public Builder rawDescription(String value) {
            snapshot.rawDescription = value;
            return this;
        }

        public Builder rawRequirements(String value) {
            snapshot.rawRequirements = value;
            return this;
        }

        public Builder rawEmploymentType(String value) {
            snapshot.rawEmploymentType = value;
            return this;
        }

        public Builder rawExperience(String value) {
            snapshot.rawExperience = value;
            return this;
        }

        public Builder rawEducation(String value) {
            snapshot.rawEducation = value;
            return this;
        }

        public Builder rawDeadline(String value) {
            snapshot.rawDeadline = value;
            return this;
        }

        public Builder rawPayload(String value) {
            snapshot.rawPayload = value;
            return this;
        }

        public Builder payloadHash(String value) {
            snapshot.payloadHash = value;
            return this;
        }

        public Builder contentHash(String value) {
            snapshot.contentHash = value;
            return this;
        }

        public JobSnapshot build() {
            return snapshot;
        }
    }
}
