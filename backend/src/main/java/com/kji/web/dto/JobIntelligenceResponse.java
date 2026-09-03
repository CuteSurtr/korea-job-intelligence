package com.kji.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobIntelligenceResponse(
        String extractorVersion,
        String roleFamily,
        String seniorityBucket,
        String seniorityLabel,
        Integer yearsExperienceMin,
        Integer yearsExperienceMax,
        String degreeRequired,
        String degreePreferred,
        String employmentType,
        String remotePolicy,
        Long salaryMin,
        Long salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        List<String> responsibilities,
        List<String> requirements,
        List<String> preferredRequirements,
        Instant extractedAt,
        List<FieldEvidence> fields,
        List<SkillEvidence> skills
) {

    public record FieldEvidence(
            String fieldName,
            String fieldValue,
            BigDecimal confidence,
            String extractionMethod,
            String evidenceText,
            Long evidenceSnapshotId,
            Instant extractedAt
    ) {
    }

    public record SkillEvidence(
            String skillSlug,
            String category,
            String requirementLevel,
            BigDecimal confidence,
            String evidenceText,
            Long evidenceSnapshotId
    ) {
    }
}
