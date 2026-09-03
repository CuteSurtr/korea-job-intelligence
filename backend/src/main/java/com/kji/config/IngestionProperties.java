package com.kji.config;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.ingestion")
public record IngestionProperties(
        boolean schedulerEnabled,
        Duration fixedDelay,
        int sourceConcurrency,
        int importMaxLines,
        Period staleAfter,
        Period unverifiedAfter
) {

    public IngestionProperties {
        fixedDelay = fixedDelay == null ? Duration.ofHours(6) : fixedDelay;
        sourceConcurrency = sourceConcurrency <= 0 ? 4 : sourceConcurrency;
        importMaxLines = importMaxLines <= 0 ? 20_000 : importMaxLines;
        staleAfter = staleAfter == null ? Period.ofDays(14) : staleAfter;
        unverifiedAfter = unverifiedAfter == null ? Period.ofDays(3) : unverifiedAfter;
    }
}
