package com.kji.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.config.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSearchResultCache implements SearchResultCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchResultCache.class);
    private static final String VERSION_KEY = "kji:cache:version";
    private static final String KEY_PREFIX = "kji:cache:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;
    private final MeterRegistry meterRegistry;
    private final AtomicLong localVersion = new AtomicLong(1);
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    public RedisSearchResultCache(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  CacheProperties properties,
                                  MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <T> Optional<T> read(String key, TypeReference<T> type) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(redisKey(key));
            recoverIfDegraded();
            if (value == null) {
                countRequest("miss");
                return Optional.empty();
            }
            countRequest("hit");
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            degrade("read", exception);
            countRequest("unavailable");
            return Optional.empty();
        }
    }

    @Override
    public void write(String key, Object value) {
        if (!properties.enabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(redisKey(key),
                    objectMapper.writeValueAsString(value), properties.ttl());
            recoverIfDegraded();
        } catch (Exception exception) {
            degrade("write", exception);
        }
    }

    @Override
    public void evictAll() {
        long next = localVersion.incrementAndGet();
        try {
            Long redisVersion = redisTemplate.opsForValue().increment(VERSION_KEY);
            localVersion.set(redisVersion == null ? next : redisVersion);
            recoverIfDegraded();
        } catch (Exception exception) {
            localVersion.set(next);
            degrade("evict", exception);
        }
    }

    @Override
    public boolean available() {
        return properties.enabled() && !degraded.get();
    }

    private String redisKey(String key) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getBytes(StandardCharsets.UTF_8));
        return KEY_PREFIX + version() + ":" + encoded;
    }

    private long version() {
        try {
            String value = redisTemplate.opsForValue().get(VERSION_KEY);
            if (value != null && !value.isBlank()) {
                long parsed = Long.parseLong(value);
                localVersion.set(parsed);
                return parsed;
            }
        } catch (Exception exception) {
            degrade("version", exception);
        }
        return localVersion.get();
    }

    private void countRequest(String result) {
        meterRegistry.counter("kji.cache.requests", "cache", "search", "result", result).increment();
    }

    private void degrade(String operation, Exception exception) {
        if (degraded.compareAndSet(false, true)) {
            log.warn("cache unavailable during {}, serving from PostgreSQL only: {}",
                    operation, exception.getMessage());
        }
    }

    private void recoverIfDegraded() {
        if (degraded.compareAndSet(true, false)) {
            log.info("cache is reachable again");
        }
    }
}
