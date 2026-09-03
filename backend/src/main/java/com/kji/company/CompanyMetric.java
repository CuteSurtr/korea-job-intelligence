package com.kji.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "company_metrics")
public class CompanyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "metric_key", nullable = false, length = 64)
    private String metricKey;

    @Column(name = "numeric_value", precision = 20, scale = 4)
    private BigDecimal numericValue;

    @Column(name = "text_value")
    private String textValue;

    @Column(length = 32)
    private String unit;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CompanyMetric() {
    }

    public CompanyMetric(Long companyId, String metricKey, BigDecimal numericValue,
                         String textValue, String unit, LocalDate effectiveDate,
                         Instant observedAt, Long sourceId, String evidenceUrl,
                         BigDecimal confidence) {
        this.companyId = companyId;
        this.metricKey = metricKey;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.unit = unit;
        this.effectiveDate = effectiveDate;
        this.observedAt = observedAt;
        this.sourceId = sourceId;
        this.evidenceUrl = evidenceUrl;
        this.confidence = confidence;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
