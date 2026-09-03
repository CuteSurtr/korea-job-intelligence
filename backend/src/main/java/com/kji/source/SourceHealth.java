package com.kji.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "source_health")
public class SourceHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "source_id", nullable = false, unique = true)
    private Source source;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_latency_ms")
    private Integer lastLatencyMs;

    @Column(name = "rolling_latency_ms")
    private Integer rollingLatencyMs;

    @Column(name = "records_last_run", nullable = false)
    private int recordsLastRun;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "total_successes", nullable = false)
    private long totalSuccesses;

    @Column(name = "total_failures", nullable = false)
    private long totalFailures;

    @Column(name = "rate_limit_events", nullable = false)
    private long rateLimitEvents;

    @Column(name = "rate_limited_until")
    private Instant rateLimitedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "circuit_state", nullable = false, length = 16)
    private CircuitState circuitState = CircuitState.CLOSED;

    @Column(name = "circuit_opened_at")
    private Instant circuitOpenedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SourceHealth() {
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void recordSuccess(Instant at, int records, long latencyMillis) {
        lastAttemptAt = at;
        lastSuccessAt = at;
        lastStatus = "SUCCESS";
        lastError = null;
        lastHttpStatus = 200;
        lastLatencyMs = clampLatency(latencyMillis);
        rollingLatencyMs = blendLatency(lastLatencyMs);
        recordsLastRun = records;
        consecutiveFailures = 0;
        totalSuccesses++;
        circuitState = CircuitState.CLOSED;
        circuitOpenedAt = null;
        updatedAt = at;
    }

    public void recordFailure(Instant at, String status, Integer httpStatus, String error,
                              long latencyMillis, int failureThreshold) {
        lastAttemptAt = at;
        lastFailureAt = at;
        lastStatus = status;
        lastHttpStatus = httpStatus;
        lastError = error;
        lastLatencyMs = clampLatency(latencyMillis);
        consecutiveFailures++;
        totalFailures++;
        if (consecutiveFailures >= failureThreshold && circuitState != CircuitState.OPEN) {
            circuitState = CircuitState.OPEN;
            circuitOpenedAt = at;
        }
        updatedAt = at;
    }

    public void recordRateLimit(Instant at, Duration retryAfter) {
        rateLimitEvents++;
        if (retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()) {
            rateLimitedUntil = at.plus(retryAfter);
        }
        updatedAt = at;
    }

    public boolean isRequestAllowed(Instant now, Duration circuitOpenDuration) {
        if (rateLimitedUntil != null && now.isBefore(rateLimitedUntil)) {
            return false;
        }
        if (circuitState == CircuitState.OPEN) {
            if (circuitOpenedAt == null || now.isAfter(circuitOpenedAt.plus(circuitOpenDuration))) {
                circuitState = CircuitState.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    private Integer clampLatency(long latencyMillis) {
        if (latencyMillis < 0) {
            return null;
        }
        return (int) Math.min(latencyMillis, Integer.MAX_VALUE);
    }

    private Integer blendLatency(Integer latest) {
        if (latest == null) {
            return rollingLatencyMs;
        }
        if (rollingLatencyMs == null) {
            return latest;
        }
        return (int) Math.round(rollingLatencyMs * 0.7d + latest * 0.3d);
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public Integer getLastLatencyMs() {
        return lastLatencyMs;
    }

    public Integer getRollingLatencyMs() {
        return rollingLatencyMs;
    }

    public int getRecordsLastRun() {
        return recordsLastRun;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public long getTotalSuccesses() {
        return totalSuccesses;
    }

    public long getTotalFailures() {
        return totalFailures;
    }

    public long getRateLimitEvents() {
        return rateLimitEvents;
    }

    public Instant getRateLimitedUntil() {
        return rateLimitedUntil;
    }

    public CircuitState getCircuitState() {
        return circuitState;
    }

    public Instant getCircuitOpenedAt() {
        return circuitOpenedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
