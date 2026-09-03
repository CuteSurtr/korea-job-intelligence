package com.kji.web.dto;

import com.kji.company.Company;
import com.kji.company.CompanyAlias;
import com.kji.company.CompanyIdentifier;
import com.kji.company.CompanyMetric;
import com.kji.company.CompanyRiskReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CompanyResponse(
        Long id,
        String canonicalName,
        String normalizedName,
        String websiteDomain,
        String countryCode,
        String industry,
        String companyType,
        LocalDate foundedOn,
        Integer employeeCount,
        String riskLevel,
        Instant riskAssessedAt,
        long openJobCount,
        List<String> aliases,
        List<Identifier> identifiers,
        List<Metric> metrics,
        List<RiskReason> riskReasons
) {

    public static CompanyResponse summary(Company company, long openJobCount) {
        return new CompanyResponse(company.getId(), company.getCanonicalName(),
                company.getNormalizedName(), company.getWebsiteDomain(), company.getCountryCode(),
                company.getIndustry(), company.getCompanyType(), company.getFoundedOn(),
                company.getEmployeeCount(), company.getRiskLevel().name(),
                company.getRiskAssessedAt(), openJobCount, List.of(), List.of(), List.of(),
                List.of());
    }

    public static CompanyResponse detail(Company company, long openJobCount,
                                         List<CompanyAlias> aliases,
                                         List<CompanyIdentifier> identifiers,
                                         List<CompanyMetric> metrics,
                                         List<CompanyRiskReason> riskReasons) {
        return new CompanyResponse(company.getId(), company.getCanonicalName(),
                company.getNormalizedName(), company.getWebsiteDomain(), company.getCountryCode(),
                company.getIndustry(), company.getCompanyType(), company.getFoundedOn(),
                company.getEmployeeCount(), company.getRiskLevel().name(),
                company.getRiskAssessedAt(), openJobCount,
                aliases.stream().map(CompanyAlias::getAlias).toList(),
                identifiers.stream().map(Identifier::from).toList(),
                metrics.stream().map(Metric::from).toList(),
                riskReasons.stream().map(RiskReason::from).toList());
    }

    public record Identifier(String type, String value, Instant observedAt) {

        static Identifier from(CompanyIdentifier identifier) {
            return new Identifier(identifier.getIdentifierType(), identifier.getIdentifierValue(),
                    identifier.getObservedAt());
        }
    }

    public record Metric(String key, BigDecimal numericValue, String textValue, String unit,
                         LocalDate effectiveDate, Instant observedAt, String evidenceUrl,
                         BigDecimal confidence) {

        static Metric from(CompanyMetric metric) {
            return new Metric(metric.getMetricKey(), metric.getNumericValue(),
                    metric.getTextValue(), metric.getUnit(), metric.getEffectiveDate(),
                    metric.getObservedAt(), metric.getEvidenceUrl(), metric.getConfidence());
        }
    }

    public record RiskReason(String riskLevel, String reasonCode, String reasonDetail,
                             Instant assessedAt, String evidence) {

        static RiskReason from(CompanyRiskReason reason) {
            return new RiskReason(reason.getRiskLevel().name(), reason.getReasonCode(),
                    reason.getReasonDetail(), reason.getAssessedAt(), reason.getEvidence());
        }
    }
}
