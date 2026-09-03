package com.kji.source;

import com.kji.config.SourceProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceHealthService {

    private static final Logger log = LoggerFactory.getLogger(SourceHealthService.class);

    private final SourceHealthRepository repository;
    private final SourceProperties properties;
    private final Clock clock;

    public SourceHealthService(SourceHealthRepository repository,
                               SourceProperties properties,
                               Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SourceHealth> all() {
        return repository.findAllWithSource();
    }

    @Transactional(readOnly = true)
    public boolean isRequestAllowed(Long sourceId) {
        return repository.findBySourceId(sourceId)
                .map(health -> health.isRequestAllowed(Instant.now(clock), properties.circuitOpenDuration()))
                .orElse(true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long sourceId, int records, long latencyMillis) {
        repository.findBySourceId(sourceId).ifPresent(health -> {
            health.recordSuccess(Instant.now(clock), records, latencyMillis);
            repository.save(health);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long sourceId, String status, Integer httpStatus,
                              String error, long latencyMillis) {
        repository.findBySourceId(sourceId).ifPresent(health -> {
            health.recordFailure(Instant.now(clock), status, httpStatus, error, latencyMillis,
                    properties.circuitFailureThreshold());
            repository.save(health);
            if (health.getCircuitState() == SourceHealth.CircuitState.OPEN) {
                log.warn("source circuit opened source={} consecutiveFailures={}",
                        health.getSource().getCode(), health.getConsecutiveFailures());
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRateLimit(Long sourceId, Duration retryAfter) {
        repository.findBySourceId(sourceId).ifPresent(health -> {
            health.recordRateLimit(Instant.now(clock), retryAfter);
            repository.save(health);
        });
    }
}
