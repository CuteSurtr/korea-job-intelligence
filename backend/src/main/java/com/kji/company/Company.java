package com.kji.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, length = 320)
    private String canonicalName;

    @Column(name = "normalized_name", nullable = false, length = 320, unique = true)
    private String normalizedName;

    @Column(name = "name_ko", length = 320)
    private String nameKo;

    @Column(name = "name_en", length = 320)
    private String nameEn;

    @Column(name = "website_domain", length = 255)
    private String websiteDomain;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 160)
    private String industry;

    @Column(name = "company_type", length = 48)
    private String companyType;

    @Column(name = "founded_on")
    private LocalDate foundedOn;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel = RiskLevel.UNKNOWN;

    @Column(name = "risk_assessed_at")
    private Instant riskAssessedAt;

    @Column(name = "risk_score_version", length = 32)
    private String riskScoreVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(String canonicalName, String normalizedName, String countryCode) {
        this.canonicalName = canonicalName;
        this.normalizedName = normalizedName;
        this.countryCode = countryCode;
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

    public Long getId() {
        return id;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getNameKo() {
        return nameKo;
    }

    public void setNameKo(String nameKo) {
        this.nameKo = nameKo;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getWebsiteDomain() {
        return websiteDomain;
    }

    public void setWebsiteDomain(String websiteDomain) {
        this.websiteDomain = websiteDomain;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getIndustry() {
        return industry;
    }

    public String getCompanyType() {
        return companyType;
    }

    public LocalDate getFoundedOn() {
        return foundedOn;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Instant getRiskAssessedAt() {
        return riskAssessedAt;
    }

    public String getRiskScoreVersion() {
        return riskScoreVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum RiskLevel {
        LOW,
        MODERATE,
        HIGH,
        UNKNOWN
    }
}
