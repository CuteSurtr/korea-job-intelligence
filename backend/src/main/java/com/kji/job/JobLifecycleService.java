package com.kji.job;

import com.kji.config.IngestionProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(JobLifecycleService.class);

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSightingRepository sightingRepository;
    private final JobVerificationRepository verificationRepository;
    private final JobLifecycleEventRepository eventRepository;
    private final IngestionProperties ingestionProperties;
    private final Clock clock;

    public JobLifecycleService(JobRepository jobRepository,
                               JobSourceRepository jobSourceRepository,
                               JobSightingRepository sightingRepository,
                               JobVerificationRepository verificationRepository,
                               JobLifecycleEventRepository eventRepository,
                               IngestionProperties ingestionProperties,
                               Clock clock) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.sightingRepository = sightingRepository;
        this.verificationRepository = verificationRepository;
        this.eventRepository = eventRepository;
        this.ingestionProperties = ingestionProperties;
        this.clock = clock;
    }

    @Transactional
    public void recordObservation(Job job, JobSource jobSource, Long searchRunId,
                                  Instant seenAt, Long snapshotId, boolean contentChanged) {
        sightingRepository.save(new JobSighting(job.getId(), jobSource.getId(), searchRunId,
                snapshotId, seenAt, contentChanged));

        JobVerification verification = verificationRepository.save(new JobVerification(
                job.getId(), jobSource.getId(), searchRunId, seenAt,
                JobVerification.Method.SOURCE_LISTING_PRESENT, JobVerification.Outcome.PRESENT,
                null, snapshotId, "Present in a successful source listing"));

        job.observeAt(seenAt);
        job.recordVerifiedPresent(seenAt);
        jobSource.markVerifiedPresent(seenAt);

        LifecycleState previous = job.getLifecycleState();
        LifecycleState next = previous == LifecycleState.CLOSED
                ? LifecycleState.REOPENED
                : LifecycleState.ACTIVE;

        if (previous == LifecycleState.CLOSED) {
            job.markReopened(seenAt);
            writeEvent(job, previous, next, "OBSERVED_AFTER_CLOSURE", seenAt,
                    searchRunId, verification.getId());
        } else if (previous != LifecycleState.ACTIVE && previous != LifecycleState.REOPENED) {
            job.applyState(next);
            writeEvent(job, previous, next, "OBSERVED_IN_SOURCE_LISTING", seenAt,
                    searchRunId, verification.getId());
        }
        jobRepository.save(job);
    }

    @Transactional
    public int reconcileCompleteListing(Long sourceId, String listingScope, Long searchRunId,
                                        Instant at, Set<String> observedExternalKeys) {
        Set<String> observed = observedExternalKeys == null ? Set.of() : new HashSet<>(observedExternalKeys);
        List<JobSource> active = jobSourceRepository.findBySourceIdAndActiveTrue(sourceId);
        String scopePrefix = listingScope == null || listingScope.isBlank()
                ? null
                : listingScope + ":";
        int closed = 0;

        for (JobSource jobSource : active) {
            if (observed.contains(jobSource.getExternalKey())) {
                continue;
            }
            if (scopePrefix != null && !jobSource.getExternalKey().startsWith(scopePrefix)) {
                continue;
            }
            JobVerification verification = verificationRepository.save(new JobVerification(
                    jobSource.getJob().getId(), jobSource.getId(), searchRunId, at,
                    JobVerification.Method.SOURCE_LISTING_ABSENT, JobVerification.Outcome.ABSENT,
                    null, null,
                    "Absent from a successful, complete source listing"));
            jobSource.markAbsent(at);
            jobSourceRepository.save(jobSource);

            Job job = jobSource.getJob();
            boolean anyStillActive = jobSourceRepository.findByJobId(job.getId()).stream()
                    .anyMatch(JobSource::isActive);
            if (!anyStillActive && job.getLifecycleState() != LifecycleState.CLOSED) {
                LifecycleState previous = job.getLifecycleState();
                job.markClosed(at, "ABSENT_FROM_ALL_SOURCES", verification.getId());
                jobRepository.save(job);
                writeEvent(job, previous, LifecycleState.CLOSED, "ABSENT_FROM_ALL_SOURCES", at,
                        searchRunId, verification.getId());
                closed++;
            }
        }
        return closed;
    }

    @Transactional
    public void recordSourceUnavailable(Job job, JobSource jobSource, Long searchRunId,
                                        Instant at, Integer httpStatus, String detail) {
        verificationRepository.save(new JobVerification(
                job.getId(), jobSource == null ? null : jobSource.getId(), searchRunId, at,
                JobVerification.Method.SOURCE_UNAVAILABLE, JobVerification.Outcome.ERROR,
                httpStatus, null, detail));
        log.debug("recorded source-unavailable verification without lifecycle change jobId={}", job.getId());
    }

    @Transactional
    public int applyStaleness(Instant now) {
        Instant unverifiedThreshold = now.minus(ingestionProperties.unverifiedAfter());
        Instant staleThreshold = now.minus(ingestionProperties.staleAfter());
        int changed = 0;

        for (Job job : jobRepository.findVerificationStale(
                List.of(LifecycleState.ACTIVE, LifecycleState.REOPENED, LifecycleState.DISCOVERED),
                unverifiedThreshold)) {
            LifecycleState previous = job.getLifecycleState();
            job.applyState(LifecycleState.UNVERIFIED);
            jobRepository.save(job);
            writeEvent(job, previous, LifecycleState.UNVERIFIED, "NO_RECENT_VERIFICATION", now, null, null);
            changed++;
        }

        for (Job job : jobRepository.findVerificationStale(
                List.of(LifecycleState.UNVERIFIED), staleThreshold)) {
            LifecycleState previous = job.getLifecycleState();
            job.applyState(LifecycleState.STALE);
            jobRepository.save(job);
            writeEvent(job, previous, LifecycleState.STALE, "PAST_STALENESS_HORIZON", now, null, null);
            changed++;
        }
        return changed;
    }

    @Transactional
    public int closeExpiredDeadlines(Instant now) {
        int closed = 0;
        for (Job job : jobRepository.findAll()) {
            if (job.getLifecycleState() == LifecycleState.CLOSED
                    || job.isDeadlineOpenEnded()
                    || job.getDeadlineAt() == null
                    || !job.getDeadlineAt().isBefore(now)) {
                continue;
            }
            JobVerification verification = verificationRepository.save(new JobVerification(
                    job.getId(), null, null, now,
                    JobVerification.Method.DEADLINE_PASSED, JobVerification.Outcome.ABSENT,
                    null, null, "Stated application deadline " + job.getDeadlineAt() + " has passed"));
            LifecycleState previous = job.getLifecycleState();
            job.markClosed(now, "DEADLINE_PASSED", verification.getId());
            jobRepository.save(job);
            writeEvent(job, previous, LifecycleState.CLOSED, "DEADLINE_PASSED", now, null,
                    verification.getId());
            closed++;
        }
        return closed;
    }

    @Transactional
    public void refreshSourceCount(Job job) {
        job.setSourceCount((int) jobSourceRepository.countByJobId(job.getId()));
        jobRepository.save(job);
    }

    private void writeEvent(Job job, LifecycleState from, LifecycleState to, String reasonCode,
                            Instant at, Long searchRunId, Long verificationId) {
        eventRepository.save(new JobLifecycleEvent(job.getId(), from, to, reasonCode, at,
                searchRunId, verificationId, "{}"));
    }

    Clock clock() {
        return clock;
    }
}
