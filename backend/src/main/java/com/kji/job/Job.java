package com.kji.job;

import com.kji.company.Company;
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

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "canonical_title", nullable = false, length = 500)
    private String canonicalTitle;

    @Column(name = "normalized_title", nullable = false, length = 500)
    private String normalizedTitle;

    @Column(name = "canonical_apply_url")
    private String canonicalApplyUrl;

    @Column(name = "canonical_url_key", length = 600)
    private String canonicalUrlKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 16)
    private LifecycleState lifecycleState = LifecycleState.DISCOVERED;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "closed_reason", length = 64)
    private String closedReason;

    @Column(name = "closed_evidence_id")
    private Long closedEvidenceId;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "deadline_open_ended", nullable = false)
    private boolean deadlineOpenEnded;

    @Column(name = "primary_source_id")
    private Long primarySourceId;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "location_raw")
    private String locationRaw;

    @Column(name = "location_city", length = 120)
    private String locationCity;

    @Column(name = "location_region", length = 120)
    private String locationRegion;

    @Column(name = "location_country", length = 2)
    private String locationCountry;

    @Column(name = "description")
    private String description;

    @Column(name = "normalized_description")
    private String normalizedDescription;

    @Column(name = "role_family", length = 48)
    private String roleFamily;

    @Column(name = "sector")
    private String sector;

    @Column(name = "company_stage")
    private String companyStage;

    @Column(name = "seniority_bucket", length = 1)
    private String seniorityBucket;

    @Column(name = "years_experience_min")
    private Integer yearsExperienceMin;

    @Column(name = "years_experience_max")
    private Integer yearsExperienceMax;

    @Column(name = "remote_policy", length = 24)
    private String remotePolicy;

    @Column(name = "employment_type", length = 32)
    private String employmentType;

    @Column(name = "degree_required", length = 48)
    private String degreeRequired;

    @Column(name = "career_value_score", precision = 6, scale = 2)
    private BigDecimal careerValueScore;

    @Column(name = "candidate_fit_score", precision = 6, scale = 2)
    private BigDecimal candidateFitScore;

    @Column(name = "application_priority_score", precision = 6, scale = 2)
    private BigDecimal applicationPriorityScore;

    @Column(name = "scored_at")
    private Instant scoredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    public Job(Company company, String canonicalTitle, String normalizedTitle,
               String canonicalApplyUrl, String canonicalUrlKey, Instant seenAt, Long primarySourceId) {
        this.company = company;
        this.canonicalTitle = canonicalTitle;
        this.normalizedTitle = normalizedTitle;
        this.canonicalApplyUrl = canonicalApplyUrl;
        this.canonicalUrlKey = canonicalUrlKey;
        this.firstSeenAt = seenAt;
        this.lastSeenAt = seenAt;
        this.primarySourceId = primarySourceId;
        this.lifecycleState = LifecycleState.DISCOVERED;
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

    public void observeAt(Instant seenAt) {
        if (seenAt.isAfter(lastSeenAt)) {
            lastSeenAt = seenAt;
        }
    }

    public void recordVerifiedPresent(Instant verifiedAt) {
        lastVerifiedAt = verifiedAt;
    }

    public void applyState(LifecycleState next) {
        this.lifecycleState = next;
    }

    public void markClosed(Instant at, String reason, Long evidenceId) {
        this.lifecycleState = LifecycleState.CLOSED;
        this.closedAt = at;
        this.closedReason = reason;
        this.closedEvidenceId = evidenceId;
    }

    public void markReopened(Instant at) {
        this.lifecycleState = LifecycleState.REOPENED;
        this.reopenedAt = at;
        this.closedReason = null;
        this.closedEvidenceId = null;
    }

    public void updateCanonicalFields(String canonicalTitle, String normalizedTitle,
                                      String canonicalApplyUrl, String canonicalUrlKey,
                                      Long primarySourceId) {
        this.canonicalTitle = canonicalTitle;
        this.normalizedTitle = normalizedTitle;
        this.canonicalApplyUrl = canonicalApplyUrl;
        this.canonicalUrlKey = canonicalUrlKey;
        this.primarySourceId = primarySourceId;
    }

    public void updateDeadline(Instant deadlineAt, boolean openEnded) {
        this.deadlineAt = deadlineAt;
        this.deadlineOpenEnded = openEnded;
    }

    public void updatePostedAt(Instant postedAt) {
        if (postedAt != null && (this.postedAt == null || postedAt.isBefore(this.postedAt))) {
            this.postedAt = postedAt;
        }
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public void updateLocation(String locationRaw, String locationCity,
                               String locationRegion, String locationCountry) {
        if (locationRaw != null) {
            this.locationRaw = locationRaw;
        }
        if (locationCity != null) {
            this.locationCity = locationCity;
        }
        if (locationRegion != null) {
            this.locationRegion = locationRegion;
        }
        if (locationCountry != null) {
            this.locationCountry = locationCountry;
        }
    }

    public void updateDescription(String description, String normalizedDescription) {
        if (description != null && (this.description == null
                || description.length() > this.description.length())) {
            this.description = description;
            this.normalizedDescription = normalizedDescription;
        }
    }

    public String getLocationRaw() {
        return locationRaw;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public String getLocationRegion() {
        return locationRegion;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public String getDescription() {
        return description;
    }

    public String getNormalizedDescription() {
        return normalizedDescription;
    }

    public void applyIntelligenceSummary(String roleFamily, String seniorityBucket,
                                         Integer yearsExperienceMin, Integer yearsExperienceMax,
                                         String remotePolicy, String employmentType,
                                         String degreeRequired, String sector) {
        this.roleFamily = roleFamily;
        this.sector = sector;
        this.seniorityBucket = seniorityBucket;
        this.yearsExperienceMin = yearsExperienceMin;
        this.yearsExperienceMax = yearsExperienceMax;
        this.remotePolicy = remotePolicy;
        this.employmentType = employmentType;
        this.degreeRequired = degreeRequired;
    }

    public void applyScores(BigDecimal careerValueScore,
                            BigDecimal candidateFitScore,
                            BigDecimal applicationPriorityScore,
                            Instant scoredAt) {
        this.careerValueScore = careerValueScore;
        this.candidateFitScore = candidateFitScore;
        this.applicationPriorityScore = applicationPriorityScore;
        this.scoredAt = scoredAt;
    }

    public void applyCompanyStage(String companyStage) {
        this.companyStage = companyStage;
    }

    public String getCompanyStage() {
        return companyStage;
    }

    public String getSector() {
        return sector;
    }

    public String getRoleFamily() {
        return roleFamily;
    }

    public String getSeniorityBucket() {
        return seniorityBucket;
    }

    public Integer getYearsExperienceMin() {
        return yearsExperienceMin;
    }

    public Integer getYearsExperienceMax() {
        return yearsExperienceMax;
    }

    public String getRemotePolicy() {
        return remotePolicy;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getDegreeRequired() {
        return degreeRequired;
    }

    public BigDecimal getCareerValueScore() {
        return careerValueScore;
    }

    public BigDecimal getCandidateFitScore() {
        return candidateFitScore;
    }

    public BigDecimal getApplicationPriorityScore() {
        return applicationPriorityScore;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getCanonicalTitle() {
        return canonicalTitle;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public String getCanonicalApplyUrl() {
        return canonicalApplyUrl;
    }

    public String getCanonicalUrlKey() {
        return canonicalUrlKey;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
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

    public Instant getReopenedAt() {
        return reopenedAt;
    }

    public String getClosedReason() {
        return closedReason;
    }

    public Long getClosedEvidenceId() {
        return closedEvidenceId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public boolean isDeadlineOpenEnded() {
        return deadlineOpenEnded;
    }

    public Long getPrimarySourceId() {
        return primarySourceId;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
