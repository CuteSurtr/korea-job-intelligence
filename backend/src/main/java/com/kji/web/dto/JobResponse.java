package com.kji.web.dto;

import com.kji.job.Job;
import java.math.BigDecimal;
import java.time.Instant;

public record JobResponse(
        Long id,
        Long companyId,
        String companyName,
        String companyRiskLevel,
        String title,
        String roleFamily,
        String sector,
        String seniorityBucket,
        Integer yearsExperienceMin,
        Integer yearsExperienceMax,
        String degreeRequired,
        String employmentType,
        String remotePolicy,
        String lifecycleState,
        String locationRaw,
        String locationCity,
        String locationRegion,
        int sourceCount,
        BigDecimal careerValueScore,
        BigDecimal candidateFitScore,
        BigDecimal applicationPriorityScore,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastVerifiedAt,
        Instant postedAt,
        Instant deadlineAt,
        boolean deadlineOpenEnded,
        Instant closedAt,
        String closedReason,
        String applyUrl
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getCompany().getId(),
                job.getCompany().getCanonicalName(),
                job.getCompany().getRiskLevel().name(),
                job.getCanonicalTitle(),
                job.getRoleFamily(),
                job.getSector(),
                job.getSeniorityBucket(),
                job.getYearsExperienceMin(),
                job.getYearsExperienceMax(),
                job.getDegreeRequired(),
                job.getEmploymentType(),
                job.getRemotePolicy(),
                job.getLifecycleState().name(),
                job.getLocationRaw(),
                job.getLocationCity(),
                job.getLocationRegion(),
                job.getSourceCount(),
                job.getCareerValueScore(),
                job.getCandidateFitScore(),
                job.getApplicationPriorityScore(),
                job.getFirstSeenAt(),
                job.getLastSeenAt(),
                job.getLastVerifiedAt(),
                job.getPostedAt(),
                job.getDeadlineAt(),
                job.isDeadlineOpenEnded(),
                job.getClosedAt(),
                job.getClosedReason(),
                job.getCanonicalApplyUrl());
    }
}
