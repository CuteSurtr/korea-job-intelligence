package com.kji.ingest;

import com.kji.config.IngestionProperties;
import com.kji.job.JobLifecycleService;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobMaintenanceScheduler.class);

    private final JobLifecycleService lifecycleService;
    private final IngestionProperties properties;
    private final Clock clock;

    public JobMaintenanceScheduler(JobLifecycleService lifecycleService,
                                   IngestionProperties properties,
                                   Clock clock) {
        this.lifecycleService = lifecycleService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${kji.ingestion.maintenance-delay:PT12H}")
    public void runMaintenance() {
        if (!properties.schedulerEnabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        int staleness = lifecycleService.applyStaleness(now);
        int closed = lifecycleService.closeExpiredDeadlines(now);
        log.info("lifecycle maintenance complete stalenessTransitions={} deadlineClosures={}",
                staleness, closed);
    }
}
