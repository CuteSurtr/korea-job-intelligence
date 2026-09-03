package com.kji.scoring;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record ScoringInput(
        Long jobId,
        String title,
        String description,
        String roleFamily,
        String seniorityBucket,
        Integer yearsExperienceMin,
        Integer yearsExperienceMax,
        String degreeRequired,
        String employmentType,
        String remotePolicy,
        String locationCity,
        String locationCountry,
        Instant deadlineAt,
        boolean deadlineOpenEnded,
        String companyRiskLevel,
        List<SkillSignal> skills
) {

    public ScoringInput {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public Set<String> skillSlugs() {
        return skills.stream().map(SkillSignal::slug).collect(java.util.stream.Collectors.toSet());
    }

    public Set<String> skillCategories() {
        return skills.stream().map(SkillSignal::category).collect(java.util.stream.Collectors.toSet());
    }

    public record SkillSignal(String slug, String category, String requirementLevel) {
    }
}
