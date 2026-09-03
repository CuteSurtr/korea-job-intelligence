package com.kji.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.source")
public record SourceProperties(
        Duration requestTimeout,
        int maxRetries,
        Duration initialBackoff,
        Duration maxBackoff,
        int circuitFailureThreshold,
        Duration circuitOpenDuration,
        String userAgent
) {

    public SourceProperties {
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
        initialBackoff = initialBackoff == null ? Duration.ofMillis(400) : initialBackoff;
        maxBackoff = maxBackoff == null ? Duration.ofSeconds(8) : maxBackoff;
        circuitOpenDuration = circuitOpenDuration == null ? Duration.ofMinutes(15) : circuitOpenDuration;
        maxRetries = Math.max(0, maxRetries);
        circuitFailureThreshold = circuitFailureThreshold <= 0 ? 5 : circuitFailureThreshold;
        userAgent = userAgent == null || userAgent.isBlank() ? "korea-job-intelligence/0.1" : userAgent;
    }
}
