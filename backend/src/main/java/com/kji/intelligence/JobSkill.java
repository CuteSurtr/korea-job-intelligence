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

@Entity
@Table(name = "job_skills")
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "skill_slug", nullable = false, length = 64)
    private String skillSlug;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_level", nullable = false, length = 16)
    private RequirementLevel requirementLevel;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "evidence_text")
    private String evidenceText;

    @Column(name = "evidence_snapshot_id")
    private Long evidenceSnapshotId;

    @Column(name = "extractor_version", nullable = false, length = 32)
    private String extractorVersion;

    protected JobSkill() {
    }

    public JobSkill(Long jobId, String skillSlug, RequirementLevel requirementLevel,
                    BigDecimal confidence, String evidenceText, Long evidenceSnapshotId,
                    String extractorVersion) {
        this.jobId = jobId;
        this.skillSlug = skillSlug;
        this.requirementLevel = requirementLevel;
        this.confidence = confidence;
        this.evidenceText = evidenceText;
        this.evidenceSnapshotId = evidenceSnapshotId;
        this.extractorVersion = extractorVersion;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getSkillSlug() {
        return skillSlug;
    }

    public RequirementLevel getRequirementLevel() {
        return requirementLevel;
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

    public String getExtractorVersion() {
        return extractorVersion;
    }

    public enum RequirementLevel {
        REQUIRED,
        PREFERRED,
        MENTIONED
    }
}
