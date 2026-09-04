package com.kji.intelligence;

public record IntelligenceInput(
        Long jobId,
        Long snapshotId,
        Long sourceId,
        String companyName,
        String title,
        String description,
        String rawExperience,
        String rawEducation,
        String rawEmploymentType,
        String rawRemotePolicy,
        String locationRaw,
        String locationCity,
        String locationRegion,
        String locationCountry
) {
}
