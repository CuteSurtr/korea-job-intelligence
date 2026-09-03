package com.kji.web.dto;

import com.kji.job.JobSource;
import java.math.BigDecimal;
import java.time.Instant;

public record JobSourceResponse(
        Long id,
        String sourceCode,
        String externalId,
        String externalKey,
        String sourceUrl,
        String applyUrl,
        boolean active,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastVerifiedAt,
        Instant closedAt,
        String matchMethod,
        BigDecimal matchConfidence,
        String matchEvidence,
        boolean manuallyCorrected,
        Long latestSnapshotId
) {

    public static JobSourceResponse from(JobSource jobSource, String sourceCode) {
        return new JobSourceResponse(
                jobSource.getId(),
                sourceCode,
                jobSource.getExternalId(),
                jobSource.getExternalKey(),
                jobSource.getSourceUrl(),
                jobSource.getApplyUrl(),
                jobSource.isActive(),
                jobSource.getFirstSeenAt(),
                jobSource.getLastSeenAt(),
                jobSource.getLastVerifiedAt(),
                jobSource.getClosedAt(),
                jobSource.getMatchMethod().name(),
                jobSource.getMatchConfidence(),
                jobSource.getMatchEvidence(),
                jobSource.isManuallyCorrected(),
                jobSource.getLatestSnapshotId());
    }
}
