package com.kji.web;

import com.kji.crm.ApplicationRepository;
import com.kji.dedupe.JobMergeCandidate;
import com.kji.dedupe.JobMergeCandidateRepository;
import com.kji.crm.ApplicationStatus;
import com.kji.ingest.SearchRunRepository;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.LifecycleState;
import com.kji.scoring.CandidateProfile;
import com.kji.scoring.CandidateProfileRepository;
import com.kji.scoring.ScoringService;
import com.kji.source.Source;
import com.kji.source.SourceHealth;
import com.kji.source.SourceHealthService;
import com.kji.source.SourceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final int RECENT_RUNS = 10;

    private final JobRepository jobRepository;
    private final SourceRepository sourceRepository;
    private final SourceHealthService healthService;
    private final SearchRunRepository searchRunRepository;
    private final ApplicationRepository applicationRepository;
    private final JobMergeCandidateRepository mergeCandidateRepository;
    private final CandidateProfileRepository profileRepository;
    private final Clock clock;

    public DashboardController(JobRepository jobRepository,
                               SourceRepository sourceRepository,
                               SourceHealthService healthService,
                               SearchRunRepository searchRunRepository,
                               ApplicationRepository applicationRepository,
                               JobMergeCandidateRepository mergeCandidateRepository,
                               CandidateProfileRepository profileRepository,
                               Clock clock) {
        this.jobRepository = jobRepository;
        this.sourceRepository = sourceRepository;
        this.healthService = healthService;
        this.searchRunRepository = searchRunRepository;
        this.applicationRepository = applicationRepository;
        this.mergeCandidateRepository = mergeCandidateRepository;
        this.profileRepository = profileRepository;
        this.clock = clock;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(@RequestParam(required = false) String profile) {
        List<Job> jobs = jobRepository.findAll();
        Instant sevenDaysAgo = Instant.now(clock).minus(7, ChronoUnit.DAYS);

        Map<LifecycleState, Long> byState = new EnumMap<>(LifecycleState.class);
        for (LifecycleState state : LifecycleState.values()) {
            byState.put(state, 0L);
        }
        long juniorAccessible = 0;
        long discoveredThisWeek = 0;
        for (Job job : jobs) {
            byState.merge(job.getLifecycleState(), 1L, Long::sum);
            if (job.getFirstSeenAt().isAfter(sevenDaysAgo)) {
                discoveredThisWeek++;
            }
            String bucket = job.getSeniorityBucket();
            if (job.getLifecycleState() != LifecycleState.CLOSED
                    && bucket != null && (bucket.equals("A") || bucket.equals("B"))) {
                juniorAccessible++;
            }
        }

        Map<String, Long> stateCounts = new LinkedHashMap<>();
        byState.forEach((state, count) -> stateCounts.put(state.name(), count));

        List<SourceHealth> health = healthService.all();
        long healthySources = health.stream()
                .filter(entry -> entry.getConsecutiveFailures() == 0
                        && entry.getCircuitState() == SourceHealth.CircuitState.CLOSED)
                .count();
        long openCircuits = health.stream()
                .filter(entry -> entry.getCircuitState() == SourceHealth.CircuitState.OPEN)
                .count();

        CandidateProfile candidateProfile = profileRepository
                .findByCode(profile == null || profile.isBlank()
                        ? ScoringService.DEFAULT_PROFILE_CODE : profile)
                .orElse(null);

        Map<String, Long> applicationCounts = new LinkedHashMap<>();
        if (candidateProfile != null) {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                applicationCounts.put(status.name(),
                        applicationRepository.countByProfileIdAndStatus(
                                candidateProfile.getId(), status));
            }
        }

        Map<Long, String> sourceCodes = new LinkedHashMap<>();
        for (Source source : sourceRepository.findAll()) {
            sourceCodes.put(source.getId(), source.getCode());
        }
        List<RecentRun> recentRuns = searchRunRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(0, RECENT_RUNS))
                .map(run -> new RecentRun(
                        run.getRunUuid().toString(),
                        sourceCodes.getOrDefault(run.getSourceId(), "unknown"),
                        run.getStatus().name(),
                        run.getStartedAt(),
                        run.getRecordsReceived(),
                        run.getNewJobs(),
                        run.getDuplicates(),
                        run.getFailures(),
                        run.getJobsClosed()))
                .getContent();

        return new DashboardResponse(
                jobs.size(),
                stateCounts,
                discoveredThisWeek,
                juniorAccessible,
                sourceRepository.count(),
                healthySources,
                openCircuits,
                mergeCandidateRepository.countByStatus(JobMergeCandidate.Status.PENDING),
                candidateProfile == null ? null : candidateProfile.getCode(),
                applicationCounts,
                recentRuns);
    }

    public record DashboardResponse(
            long totalJobs,
            Map<String, Long> jobsByLifecycleState,
            long jobsDiscoveredLastSevenDays,
            long juniorAccessibleOpenJobs,
            long sourceCount,
            long healthySources,
            long openCircuits,
            long pendingMergeReviews,
            String profileCode,
            Map<String, Long> applicationsByStatus,
            List<RecentRun> recentRuns
    ) {
    }

    public record RecentRun(
            String runUuid,
            String sourceCode,
            String status,
            Instant startedAt,
            int recordsReceived,
            int newJobs,
            int duplicates,
            int failures,
            int jobsClosed
    ) {
    }
}
