package com.kji.ingest;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class IngestionMetrics {

    private static final String SOURCE_TAG = "source";

    private final MeterRegistry registry;

    public IngestionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRun(String sourceCode, IngestionPipeline.ProcessingSummary summary, int closed) {
        registry.counter("kji.ingestion.records.normalized", SOURCE_TAG, sourceCode)
                .increment(summary.normalized());
        registry.counter("kji.ingestion.jobs.discovered", SOURCE_TAG, sourceCode)
                .increment(summary.created());
        registry.counter("kji.ingestion.jobs.updated", SOURCE_TAG, sourceCode)
                .increment(summary.updated());
        registry.counter("kji.ingestion.jobs.duplicates", SOURCE_TAG, sourceCode)
                .increment(summary.duplicates());
        registry.counter("kji.ingestion.jobs.closed", SOURCE_TAG, sourceCode)
                .increment(closed);
    }

    public void recordRunDuration(String sourceCode, long durationMillis) {
        Timer.builder("kji.ingestion.run.duration")
                .tag(SOURCE_TAG, sourceCode)
                .register(registry)
                .record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordSourceLatency(String sourceCode, long latencyMillis) {
        Timer.builder("kji.source.request.latency")
                .tag(SOURCE_TAG, sourceCode)
                .register(registry)
                .record(latencyMillis, TimeUnit.MILLISECONDS);
    }

    public void recordSourceFailure(String sourceCode) {
        registry.counter("kji.source.failures", SOURCE_TAG, sourceCode).increment();
    }

    public void recordRateLimit(String sourceCode) {
        registry.counter("kji.source.rate.limits", SOURCE_TAG, sourceCode).increment();
    }

    public void recordNormalizationFailure(String sourceCode) {
        registry.counter("kji.ingestion.normalization.failures", SOURCE_TAG, sourceCode).increment();
    }

    public void recordIngestFailure(String sourceCode) {
        registry.counter("kji.ingestion.record.failures", SOURCE_TAG, sourceCode).increment();
    }

    public void recordCacheHit(String cache, boolean hit) {
        registry.counter("kji.cache.requests", "cache", cache, "result", hit ? "hit" : "miss")
                .increment();
    }
}
