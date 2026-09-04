package com.kji.intelligence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_intelligence")
public class JobIntelligence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private Long jobId;

    @Column(name = "extractor_version", nullable = false, length = 32)
    private String extractorVersion;

    @Column(name = "source_snapshot_id")
    private Long sourceSnapshotId;

    @Column(name = "role_family", length = 48)
    private String roleFamily;

    @Column(name = "sector")
    private String sector;

    @Column(name = "sector_confidence")
    private java.math.BigDecimal sectorConfidence;

    @Column(name = "seniority_bucket", length = 1)
    private String seniorityBucket;

    @Column(name = "seniority_label", length = 48)
    private String seniorityLabel;

    @Column(name = "years_experience_min")
    private Integer yearsExperienceMin;

    @Column(name = "years_experience_max")
    private Integer yearsExperienceMax;

    @Column(name = "degree_required", length = 48)
    private String degreeRequired;

    @Column(name = "degree_preferred", length = 48)
    private String degreePreferred;

    @Column(name = "employment_type", length = 32)
    private String employmentType;

    @Column(name = "remote_policy", length = 24)
    private String remotePolicy;

    @Column(name = "location_country", length = 2)
    private String locationCountry;

    @Column(name = "location_region", length = 120)
    private String locationRegion;

    @Column(name = "location_city", length = 120)
    private String locationCity;

    @Column(name = "location_raw")
    private String locationRaw;

    @Column(name = "salary_min")
    private Long salaryMin;

    @Column(name = "salary_max")
    private Long salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency;

    @Column(name = "salary_period", length = 16)
    private String salaryPeriod;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] responsibilities = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] requirements = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_requirements", nullable = false, columnDefinition = "text[]")
    private String[] preferredRequirements = new String[0];

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobIntelligence() {
    }

    public JobIntelligence(Long jobId, String extractorVersion, Instant extractedAt) {
        this.jobId = jobId;
        this.extractorVersion = extractorVersion;
        this.extractedAt = extractedAt;
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

    public void apply(String extractorVersion, Long sourceSnapshotId, Instant extractedAt) {
        this.extractorVersion = extractorVersion;
        this.sourceSnapshotId = sourceSnapshotId;
        this.extractedAt = extractedAt;
    }

    public void setRoleFamily(String roleFamily) {
        this.roleFamily = roleFamily;
    }

    public void setSector(String sector, java.math.BigDecimal confidence) {
        this.sector = sector;
        this.sectorConfidence = confidence;
    }

    public String getSector() {
        return sector;
    }

    public java.math.BigDecimal getSectorConfidence() {
        return sectorConfidence;
    }

    public void setSeniority(String bucket, String label) {
        this.seniorityBucket = bucket;
        this.seniorityLabel = label;
    }

    public void setYearsExperience(Integer min, Integer max) {
        this.yearsExperienceMin = min;
        this.yearsExperienceMax = max;
    }

    public void setDegree(String required, String preferred) {
        this.degreeRequired = required;
        this.degreePreferred = preferred;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public void setRemotePolicy(String remotePolicy) {
        this.remotePolicy = remotePolicy;
    }

    public void setLocation(String raw, String city, String region, String country) {
        this.locationRaw = raw;
        this.locationCity = city;
        this.locationRegion = region;
        this.locationCountry = country;
    }

    public void setSalary(Long min, Long max, String currency, String period) {
        this.salaryMin = min;
        this.salaryMax = max;
        this.salaryCurrency = currency;
        this.salaryPeriod = period;
    }

    public void setSections(String[] responsibilities, String[] requirements,
                            String[] preferredRequirements) {
        this.responsibilities = responsibilities == null ? new String[0] : responsibilities.clone();
        this.requirements = requirements == null ? new String[0] : requirements.clone();
        this.preferredRequirements =
                preferredRequirements == null ? new String[0] : preferredRequirements.clone();
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getExtractorVersion() {
        return extractorVersion;
    }

    public Long getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public String getRoleFamily() {
        return roleFamily;
    }

    public String getSeniorityBucket() {
        return seniorityBucket;
    }

    public String getSeniorityLabel() {
        return seniorityLabel;
    }

    public Integer getYearsExperienceMin() {
        return yearsExperienceMin;
    }

    public Integer getYearsExperienceMax() {
        return yearsExperienceMax;
    }

    public String getDegreeRequired() {
        return degreeRequired;
    }

    public String getDegreePreferred() {
        return degreePreferred;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getRemotePolicy() {
        return remotePolicy;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public String getLocationRegion() {
        return locationRegion;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public String getLocationRaw() {
        return locationRaw;
    }

    public Long getSalaryMin() {
        return salaryMin;
    }

    public Long getSalaryMax() {
        return salaryMax;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public String getSalaryPeriod() {
        return salaryPeriod;
    }

    public String[] getResponsibilities() {
        return responsibilities.clone();
    }

    public String[] getRequirements() {
        return requirements.clone();
    }

    public String[] getPreferredRequirements() {
        return preferredRequirements.clone();
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }
}
