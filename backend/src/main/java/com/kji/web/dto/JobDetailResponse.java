package com.kji.web.dto;

import java.time.Instant;
import java.util.List;

public record JobDetailResponse(
        JobResponse job,
        String description,
        JobIntelligenceResponse intelligence,
        List<JobScoreResponse> scores,
        List<JobSourceResponse> sources,
        List<SnapshotSummary> snapshots,
        List<VerificationSummary> verifications,
        List<LifecycleEventSummary> lifecycle
) {

    public record SnapshotSummary(
            Long id,
            String sourceCode,
            String externalId,
            String sourceUrl,
            Instant fetchedAt,
            String contentHash,
            String rawTitle,
            String rawCompany,
            String rawLocation,
            String rawExperience,
            String rawEducation,
            String rawDeadline
    ) {
    }

    public record VerificationSummary(
            Long id,
            Instant verifiedAt,
            String method,
            String outcome,
            Integer httpStatus,
            Long snapshotId,
            String detail
    ) {
    }

    public record LifecycleEventSummary(
            Long id,
            String fromState,
            String toState,
            String reasonCode,
            Instant occurredAt,
            Long verificationId
    ) {
    }
}
