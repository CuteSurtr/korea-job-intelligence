package com.kji.config;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.ingestion")
public record IngestionProperties(
        boolean schedulerEnabled,
        Duration fixedDelay,
        Duration maintenanceDelay,
        int sourceConcurrency,
        int importMaxLines,
        Period staleAfter,
        Period unverifiedAfter,
        List<Target> targets
) {

    public IngestionProperties {
        fixedDelay = fixedDelay == null ? Duration.ofHours(6) : fixedDelay;
        maintenanceDelay = maintenanceDelay == null ? Duration.ofHours(12) : maintenanceDelay;
        sourceConcurrency = sourceConcurrency <= 0 ? 4 : sourceConcurrency;
        importMaxLines = importMaxLines <= 0 ? 20_000 : importMaxLines;
        staleAfter = staleAfter == null ? Period.ofDays(14) : staleAfter;
        unverifiedAfter = unverifiedAfter == null ? Period.ofDays(3) : unverifiedAfter;
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public record Target(String source, String board, String company, String location,
                         String query, Integer maxRecords, Boolean enabled) {

        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        public int recordLimit() {
            return maxRecords == null || maxRecords <= 0 ? 200 : maxRecords;
        }

        public Map<String, String> parameters() {
            java.util.LinkedHashMap<String, String> parameters = new java.util.LinkedHashMap<>();
            if (board != null) {
                parameters.put("board", board);
            }
            if (company != null) {
                parameters.put("company", company);
            }
            if (location != null) {
                parameters.put("location", location);
            }
            return parameters;
        }
    }
}
