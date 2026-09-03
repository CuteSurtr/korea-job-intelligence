package com.kji.web.dto;

import com.kji.ingest.IngestionFailure;
import com.kji.ingest.SearchRun;
import java.time.Instant;
import java.util.List;

public record SearchRunResponse(
        Long id,
        String runUuid,
        String sourceCode,
        String triggerKind,
        String queryText,
        String status,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        int recordsReceived,
        int recordsNormalized,
        int newJobs,
        int updatedJobs,
        int duplicates,
        int failures,
        int rateLimitEvents,
        int jobsClosed,
        String errorSummary,
        String collector,
        List<FailureSummary> failureDetails
) {

    public static SearchRunResponse from(SearchRun run, String sourceCode,
                                         List<FailureSummary> failureDetails) {
        return new SearchRunResponse(
                run.getId(),
                run.getRunUuid().toString(),
                sourceCode,
                run.getTriggerKind().name(),
                run.getQueryText(),
                run.getStatus().name(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getDurationMs(),
                run.getRecordsReceived(),
                run.getRecordsNormalized(),
                run.getNewJobs(),
                run.getUpdatedJobs(),
                run.getDuplicates(),
                run.getFailures(),
                run.getRateLimitEvents(),
                run.getJobsClosed(),
                run.getErrorSummary(),
                run.getCollector(),
                failureDetails);
    }

    public record FailureSummary(
            Long id,
            String stage,
            String reasonCode,
            String message,
            String externalId,
            String sourceUrl,
            Instant occurredAt
    ) {

        public static FailureSummary from(IngestionFailure failure) {
            return new FailureSummary(
                    failure.getId(),
                    failure.getStage().name(),
                    failure.getReasonCode(),
                    failure.getMessage(),
                    failure.getExternalId(),
                    failure.getSourceUrl(),
                    failure.getOccurredAt());
        }
    }
}
