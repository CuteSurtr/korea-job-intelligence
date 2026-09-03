package com.kji.web.dto;

import com.kji.source.Source;
import com.kji.source.SourceHealth;
import java.time.Instant;

public record SourceResponse(
        Long id,
        String code,
        String displayName,
        String adapterKind,
        boolean runtimeAvailable,
        short trustTier,
        boolean stableExternalId,
        boolean providesFullDescription,
        boolean enabled,
        String baseUrl,
        boolean adapterRegistered
) {

    public static SourceResponse from(Source source, boolean adapterRegistered) {
        return new SourceResponse(
                source.getId(),
                source.getCode(),
                source.getDisplayName(),
                source.getAdapterKind().name(),
                source.isRuntimeAvailable(),
                source.getTrustTier(),
                source.isStableExternalId(),
                source.isProvidesFullDescription(),
                source.isEnabled(),
                source.getBaseUrl(),
                adapterRegistered);
    }

    public record Health(
            String sourceCode,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            Instant lastAttemptAt,
            String lastStatus,
            Integer lastHttpStatus,
            String lastError,
            Integer lastLatencyMs,
            Integer rollingLatencyMs,
            int recordsLastRun,
            int consecutiveFailures,
            long totalSuccesses,
            long totalFailures,
            long rateLimitEvents,
            Instant rateLimitedUntil,
            String circuitState,
            Instant circuitOpenedAt,
            Instant updatedAt
    ) {

        public static Health from(SourceHealth health) {
            return new Health(
                    health.getSource().getCode(),
                    health.getLastSuccessAt(),
                    health.getLastFailureAt(),
                    health.getLastAttemptAt(),
                    health.getLastStatus(),
                    health.getLastHttpStatus(),
                    health.getLastError(),
                    health.getLastLatencyMs(),
                    health.getRollingLatencyMs(),
                    health.getRecordsLastRun(),
                    health.getConsecutiveFailures(),
                    health.getTotalSuccesses(),
                    health.getTotalFailures(),
                    health.getRateLimitEvents(),
                    health.getRateLimitedUntil(),
                    health.getCircuitState().name(),
                    health.getCircuitOpenedAt(),
                    health.getUpdatedAt());
        }
    }
}
