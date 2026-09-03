package com.kji.ingest;

import java.util.List;

public record IngestionOutcome(
        Long searchRunId,
        String runUuid,
        String sourceCode,
        int recordsReceived,
        int recordsNormalized,
        int newJobs,
        int updatedJobs,
        int duplicates,
        int failures,
        int rateLimitEvents,
        int jobsClosed,
        long durationMillis,
        SearchRun.Status status,
        List<String> failureReasons
) {

    public IngestionOutcome {
        failureReasons = failureReasons == null ? List.of() : List.copyOf(failureReasons);
    }
}
