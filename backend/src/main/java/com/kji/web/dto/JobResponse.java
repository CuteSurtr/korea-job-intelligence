package com.kji.web.dto;

import com.kji.job.Job;
import java.time.Instant;

public record JobResponse(
        Long id,
        Long companyId,
        String companyName,
        String title,
        String lifecycleState,
        String locationRaw,
        String locationCity,
        String locationRegion,
        int sourceCount,
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
                job.getCanonicalTitle(),
                job.getLifecycleState().name(),
                job.getLocationRaw(),
                job.getLocationCity(),
                job.getLocationRegion(),
                job.getSourceCount(),
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
