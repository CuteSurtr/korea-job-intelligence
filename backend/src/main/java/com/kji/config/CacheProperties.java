package com.kji.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.cache")
public record CacheProperties(boolean enabled, Duration ttl) {

    public CacheProperties {
        ttl = ttl == null ? Duration.ofMinutes(10) : ttl;
    }
}
