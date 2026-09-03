package com.kji.intelligence;

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

@Entity
@Table(name = "job_intelligence_fields")
public class JobIntelligenceField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "field_name", nullable = false, length = 64)
    private String fieldName;

    @Column(name = "field_value")
    private String fieldValue;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "evidence_text")
    private String evidenceText;

    @Column(name = "evidence_snapshot_id")
    private Long evidenceSnapshotId;

    @Column(name = "evidence_source_id")
    private Long evidenceSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method", nullable = false, length = 48)
    private ExtractionMethod extractionMethod;

    @Column(name = "extractor_version", nullable = false, length = 32)
    private String extractorVersion;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    protected JobIntelligenceField() {
    }

    public JobIntelligenceField(Long jobId, String fieldName, String fieldValue,
                                BigDecimal confidence, String evidenceText,
                                Long evidenceSnapshotId, Long evidenceSourceId,
                                ExtractionMethod extractionMethod, String extractorVersion,
                                Instant extractedAt) {
        this.jobId = jobId;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.confidence = confidence;
        this.evidenceText = evidenceText;
        this.evidenceSnapshotId = evidenceSnapshotId;
        this.evidenceSourceId = evidenceSourceId;
        this.extractionMethod = extractionMethod;
        this.extractorVersion = extractorVersion;
        this.extractedAt = extractedAt;
    }

    public void update(String fieldValue, BigDecimal confidence, String evidenceText,
                       Long evidenceSnapshotId, Long evidenceSourceId,
                       ExtractionMethod extractionMethod, Instant extractedAt) {
        this.fieldValue = fieldValue;
        this.confidence = confidence;
        this.evidenceText = evidenceText;
        this.evidenceSnapshotId = evidenceSnapshotId;
        this.evidenceSourceId = evidenceSourceId;
        this.extractionMethod = extractionMethod;
        this.extractedAt = extractedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public Long getEvidenceSnapshotId() {
        return evidenceSnapshotId;
    }

    public Long getEvidenceSourceId() {
        return evidenceSourceId;
    }

    public ExtractionMethod getExtractionMethod() {
        return extractionMethod;
    }

    public String getExtractorVersion() {
        return extractorVersion;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public enum ExtractionMethod {
        SOURCE_STRUCTURED,
        PATTERN_MATCH,
        LEXICON,
        HEURISTIC,
        MANUAL
    }
}
