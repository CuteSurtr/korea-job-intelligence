package com.kji.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.cache.SearchResultCache;
import com.kji.job.JobLifecycleService;
import com.kji.source.RawJobRecord;
import com.kji.source.Source;
import com.kji.source.SourceAdapter;
import com.kji.source.SourceAdapterRegistry;
import com.kji.source.SourceException;
import com.kji.source.SourceFetchResult;
import com.kji.source.SourceHealthService;
import com.kji.source.SourceQuery;
import com.kji.source.SourceRateLimitedException;
import com.kji.source.SourceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);
    private static final String MDC_RUN_ID = "ingestionRunId";
    private static final String MDC_SOURCE = "sourceCode";

    private final SourceRepository sourceRepository;
    private final SourceAdapterRegistry adapterRegistry;
    private final SourceHealthService healthService;
    private final SearchRunService searchRunService;
    private final RecordIngestor recordIngestor;
    private final IngestionFailureWriter failureWriter;
    private final JobLifecycleService lifecycleService;
    private final IngestionMetrics metrics;
    private final SearchResultCache cache;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IngestionPipeline(SourceRepository sourceRepository,
                             SourceAdapterRegistry adapterRegistry,
                             SourceHealthService healthService,
                             SearchRunService searchRunService,
                             RecordIngestor recordIngestor,
                             IngestionFailureWriter failureWriter,
                             JobLifecycleService lifecycleService,
                             IngestionMetrics metrics,
                             SearchResultCache cache,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.sourceRepository = sourceRepository;
        this.adapterRegistry = adapterRegistry;
        this.healthService = healthService;
        this.searchRunService = searchRunService;
        this.recordIngestor = recordIngestor;
        this.failureWriter = failureWriter;
        this.lifecycleService = lifecycleService;
        this.metrics = metrics;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public IngestionOutcome runDirect(String sourceCode, SourceQuery query,
                                      SearchRun.TriggerKind trigger) {
        Source source = requireSource(sourceCode);
        if (!source.isRuntimeAvailable()) {
            throw new SourceException("Source " + sourceCode
                    + " is not runtime-available; use the import boundary");
        }
        SourceAdapter adapter = adapterRegistry.require(sourceCode);
        SearchRun run = searchRunService.open(source, trigger, query.queryText(),
                serialize(query.parameters()), null);

        MDC.put(MDC_RUN_ID, run.getRunUuid().toString());
        MDC.put(MDC_SOURCE, sourceCode);
        try {
            if (!healthService.isRequestAllowed(source.getId())) {
                log.warn("skipping run: source unavailable by circuit or rate limit source={}", sourceCode);
                failureWriter.recordFetchFailure(run.getId(), source.getId(),
                        "SOURCE_UNAVAILABLE", "Circuit open or rate limit window active");
                return finish(run, SearchRun.Status.SKIPPED, "Circuit open or rate limit window active");
            }

            SourceFetchResult fetch;
            try {
                fetch = adapter.fetch(query);
            } catch (SourceRateLimitedException exception) {
                healthService.recordRateLimit(source.getId(), exception.retryAfter());
                healthService.recordFailure(source.getId(), "RATE_LIMITED",
                        exception.httpStatus(), exception.getMessage(), 0L);
                failureWriter.recordFetchFailure(run.getId(), source.getId(),
                        "RATE_LIMITED", exception.getMessage());
                metrics.recordRateLimit(sourceCode);
                run.addCounters(0, 0, 0, 0, 0, 1, 1);
                return finish(run, SearchRun.Status.FAILED, exception.getMessage());
            } catch (SourceException exception) {
                healthService.recordFailure(source.getId(), "FETCH_FAILED",
                        exception.httpStatus(), exception.getMessage(), 0L);
                failureWriter.recordFetchFailure(run.getId(), source.getId(),
                        "FETCH_FAILED", exception.getMessage());
                metrics.recordSourceFailure(sourceCode);
                run.addCounters(0, 0, 0, 0, 0, 1, 0);
                return finish(run, SearchRun.Status.FAILED, exception.getMessage());
            }

            healthService.recordSuccess(source.getId(), fetch.records().size(), fetch.latencyMillis());
            metrics.recordSourceLatency(sourceCode, fetch.latencyMillis());

            ProcessingSummary summary = processRecords(source, run, fetch.records());

            int closed = 0;
            if (fetch.listingComplete() && isFullListing(query)) {
                closed = lifecycleService.reconcileCompleteListing(
                        source.getId(), run.getId(), Instant.now(clock), summary.observedKeys());
                run.addJobsClosed(closed);
            }

            run.addCounters(fetch.records().size(), summary.normalized(), summary.created(),
                    summary.updated(), summary.duplicates(), summary.failures(),
                    fetch.rateLimitEvents());
            metrics.recordRun(sourceCode, summary, closed);

            SearchRun.Status status = summary.failures() == 0
                    ? SearchRun.Status.SUCCEEDED
                    : SearchRun.Status.PARTIAL;
            return finish(run, status, summary.failures() == 0 ? null
                    : summary.failures() + " record(s) failed");
        } finally {
            MDC.remove(MDC_RUN_ID);
            MDC.remove(MDC_SOURCE);
        }
    }

    public IngestionOutcome runImport(String sourceCode, List<RawJobRecord> records, String collector) {
        return runImport(sourceCode, records, collector, List.of());
    }

    public IngestionOutcome runImport(String sourceCode, List<RawJobRecord> records,
                                      String collector, List<MalformedLine> malformedLines) {
        Source source = requireSource(sourceCode);
        SearchRun run = searchRunService.open(source, SearchRun.TriggerKind.IMPORT, null, "{}", collector);

        MDC.put(MDC_RUN_ID, run.getRunUuid().toString());
        MDC.put(MDC_SOURCE, sourceCode);
        try {
            for (MalformedLine malformed : malformedLines) {
                failureWriter.recordLineFailure(run.getId(), source.getId(),
                        malformed.reasonCode(), malformed.message(), malformed.line());
                metrics.recordNormalizationFailure(sourceCode);
            }

            ProcessingSummary summary = processRecords(source, run, records);
            int totalFailures = summary.failures() + malformedLines.size();
            run.addCounters(records.size() + malformedLines.size(), summary.normalized(),
                    summary.created(), summary.updated(), summary.duplicates(), totalFailures, 0);
            metrics.recordRun(sourceCode, summary, 0);
            healthService.recordSuccess(source.getId(), records.size(), 0L);

            SearchRun.Status status = totalFailures == 0
                    ? SearchRun.Status.SUCCEEDED
                    : SearchRun.Status.PARTIAL;
            return finish(run, status, totalFailures == 0 ? null
                    : totalFailures + " record(s) failed");
        } finally {
            MDC.remove(MDC_RUN_ID);
            MDC.remove(MDC_SOURCE);
        }
    }

    private ProcessingSummary processRecords(Source source, SearchRun run, List<RawJobRecord> records) {
        Set<String> observedKeys = new HashSet<>();
        List<String> failureReasons = new ArrayList<>();
        int normalized = 0;
        int created = 0;
        int updated = 0;
        int duplicates = 0;
        int failures = 0;

        for (RawJobRecord record : records) {
            if (record == null || !record.hasMinimumFields()) {
                failures++;
                failureReasons.add("MISSING_REQUIRED_FIELD");
                failureWriter.recordRecordFailure(run.getId(), source.getId(),
                        IngestionFailure.Stage.NORMALIZE, "MISSING_REQUIRED_FIELD",
                        "Record is missing source, title, company or fetchedAt", record);
                metrics.recordNormalizationFailure(source.getCode());
                continue;
            }
            try {
                RecordIngestionResult result = recordIngestor.ingest(source, run, record);
                normalized++;
                observedKeys.add(result.externalKey());
                switch (result.outcome()) {
                    case JOB_CREATED -> created++;
                    case JOB_UPDATED -> updated++;
                    case DUPLICATE_MERGED -> duplicates++;
                    case FAILED -> failures++;
                }
            } catch (RuntimeException exception) {
                failures++;
                failureReasons.add(exception.getClass().getSimpleName());
                failureWriter.recordRecordFailure(run.getId(), source.getId(),
                        IngestionFailure.Stage.PERSIST, "RECORD_INGEST_FAILED",
                        exception.getMessage() == null ? exception.toString() : exception.getMessage(),
                        record);
                metrics.recordIngestFailure(source.getCode());
                log.warn("record ingestion failed source={} externalId={} reason={}",
                        source.getCode(), record.externalId(), exception.toString());
            }
        }
        return new ProcessingSummary(normalized, created, updated, duplicates, failures,
                observedKeys, failureReasons);
    }

    private boolean isFullListing(SourceQuery query) {
        return query.queryText() == null || query.queryText().isBlank();
    }

    private IngestionOutcome finish(SearchRun run, SearchRun.Status status, String errorSummary) {
        SearchRun saved = searchRunService.complete(run, status, errorSummary);
        String sourceCode = sourceRepository.findById(saved.getSourceId())
                .map(Source::getCode)
                .orElse("unknown");

        log.info("ingestion run complete source={} run={} status={} received={} normalized={} "
                        + "new={} updated={} duplicates={} failures={} closed={} durationMs={}",
                sourceCode, saved.getRunUuid(), saved.getStatus(), saved.getRecordsReceived(),
                saved.getRecordsNormalized(), saved.getNewJobs(), saved.getUpdatedJobs(),
                saved.getDuplicates(), saved.getFailures(), saved.getJobsClosed(),
                saved.getDurationMs());

        metrics.recordRunDuration(sourceCode, saved.getDurationMs() == null ? 0L : saved.getDurationMs());

        if (saved.getNewJobs() > 0 || saved.getUpdatedJobs() > 0 || saved.getJobsClosed() > 0
                || saved.getDuplicates() > 0) {
            cache.evictAll();
        }

        return new IngestionOutcome(saved.getId(), saved.getRunUuid().toString(), sourceCode,
                saved.getRecordsReceived(), saved.getRecordsNormalized(), saved.getNewJobs(),
                saved.getUpdatedJobs(), saved.getDuplicates(), saved.getFailures(),
                saved.getRateLimitEvents(), saved.getJobsClosed(),
                saved.getDurationMs() == null ? 0L : saved.getDurationMs(),
                saved.getStatus(), List.of());
    }

    private Source requireSource(String sourceCode) {
        return sourceRepository.findByCode(sourceCode)
                .orElseThrow(() -> new SourceException("Unknown source code " + sourceCode));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public record ProcessingSummary(
            int normalized,
            int created,
            int updated,
            int duplicates,
            int failures,
            Set<String> observedKeys,
            List<String> failureReasons
    ) {
    }
}
