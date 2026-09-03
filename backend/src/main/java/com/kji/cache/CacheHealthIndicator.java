package com.kji.cache;

import com.kji.config.CacheProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("cache")
public class CacheHealthIndicator implements HealthIndicator {

    private final SearchResultCache cache;
    private final CacheProperties properties;

    public CacheHealthIndicator(SearchResultCache cache, CacheProperties properties) {
        this.cache = cache;
        this.properties = properties;
    }

    @Override
    public Health health() {
        boolean available = cache.available();
        return Health.up()
                .withDetail("enabled", properties.enabled())
                .withDetail("available", available)
                .withDetail("mode", available ? "cached" : "postgresql-only")
                .build();
    }
}
