package com.kji.ingest;

import com.kji.config.IngestionProperties;
import com.kji.source.SourceException;
import com.kji.source.SourceQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final IngestionPipeline pipeline;
    private final IngestionProperties properties;

    public IngestionScheduler(IngestionPipeline pipeline, IngestionProperties properties) {
        this.pipeline = pipeline;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${kji.ingestion.fixed-delay:PT6H}")
    public void runScheduledIngestion() {
        if (!properties.schedulerEnabled()) {
            return;
        }
        List<IngestionProperties.Target> targets = properties.targets().stream()
                .filter(IngestionProperties.Target::isEnabled)
                .toList();
        if (targets.isEmpty()) {
            log.info("scheduled ingestion has no enabled targets configured");
            return;
        }

        int threads = Math.min(properties.sourceConcurrency(), targets.size());
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (IngestionProperties.Target target : targets) {
                futures.add(executor.submit(() -> runTarget(target)));
            }
            for (Future<?> future : futures) {
                awaitQuietly(future);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void runTarget(IngestionProperties.Target target) {
        try {
            SourceQuery query = new SourceQuery(target.query(), target.parameters(),
                    target.recordLimit());
            pipeline.runDirect(target.source(), query, SearchRun.TriggerKind.SCHEDULED);
        } catch (SourceException exception) {
            log.warn("scheduled ingestion target failed source={} board={} reason={}",
                    target.source(), target.board(), exception.getMessage());
        }
    }

    private void awaitQuietly(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            log.warn("scheduled ingestion task failed: {}", exception.getMessage());
        }
    }
}
