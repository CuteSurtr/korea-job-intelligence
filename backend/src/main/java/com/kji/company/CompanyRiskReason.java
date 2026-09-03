package com.kji.company;

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
@Table(name = "company_risk_reasons")
public class CompanyRiskReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private Company.RiskLevel riskLevel;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "reason_detail", nullable = false)
    private String reasonDetail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidence = "{}";

    protected CompanyRiskReason() {
    }

    public CompanyRiskReason(Long companyId, Instant assessedAt, Company.RiskLevel riskLevel,
                             String reasonCode, String reasonDetail, String evidence) {
        this.companyId = companyId;
        this.assessedAt = assessedAt;
        this.riskLevel = riskLevel;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.evidence = evidence == null ? "{}" : evidence;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public Company.RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public String getEvidence() {
        return evidence;
    }
}
