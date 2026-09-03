package com.kji.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.kji.company.CompanyRepository;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.JobSightingRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import com.kji.job.LifecycleState;
import com.kji.snapshot.JobSnapshot;
import com.kji.snapshot.JobSnapshotRepository;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImportIngestionIntegrationTest extends AbstractIntegrationTest {

    private static final String JOBKOREA_LINE = """
            {"sourceCode":"jobkorea","externalId":"49779953",\
            "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49779953",\
            "originalApplyUrl":"https://toss.im/career/job-detail?job_id=5310711003",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"[토스] Systems Engineer (GPU)",\
            "rawCompany":"㈜바바리퍼블리카","rawLocation":"서울 서초구",\
            "rawExperience":"경력 3년 이상","rawEducation":"학력무관",\
            "rawDeadline":"2026. 10. 12.","rawPayload":{"board":"jobkorea"}}""";

    private static final String SARAMIN_LINE = """
            {"sourceCode":"saramin","externalId":"54741770",\
            "sourceUrl":"https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=54741770",\
            "originalApplyUrl":"https://toss.im/career/job-detail?job_id=5310711003&utm_source=saramin",\
            "fetchedAt":"2026-09-03T00:05:00Z","rawTitle":"[토스] Systems Engineer (GPU)",\
            "rawCompany":"(주)바바리퍼블리카","rawLocation":"서울  서초구",\
            "rawExperience":"경력3년↑","rawEducation":"학력무관",\
            "rawDeadline":"~ 10/12(월)","rawPayload":{"board":"saramin"}}""";

    private static final String OPEN_ENDED_LINE = """
            {"sourceCode":"jobkorea","externalId":"49835112",\
            "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49835112",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"[토스] Recruiting Assistant",\
            "rawCompany":"㈜바바리퍼블리카","rawLocation":"서울 강남구",\
            "rawExperience":"신입","rawDeadline":"2069. 12. 31.","rawPayload":{}}""";

    @Autowired
    private ImportService importService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private JobSnapshotRepository snapshotRepository;

    @Autowired
    private JobSightingRepository sightingRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SearchRunRepository searchRunRepository;

    @Autowired
    private IngestionFailureRepository failureRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("an imported record becomes one canonical job with its raw snapshot preserved")
    void importCreatesCanonicalJobWithProvenance() {
        IngestionOutcome outcome = importLines("jobkorea", JOBKOREA_LINE);

        assertThat(outcome.status()).isEqualTo(SearchRun.Status.SUCCEEDED);
        assertThat(outcome.recordsReceived()).isEqualTo(1);
        assertThat(outcome.newJobs()).isEqualTo(1);

        Job job = jobRepository.findAll().get(0);
        assertThat(job.getCanonicalTitle()).isEqualTo("[토스] Systems Engineer (GPU)");
        assertThat(job.getLifecycleState()).isEqualTo(LifecycleState.ACTIVE);
        assertThat(job.getLocationCity()).isEqualTo("Seoul");
        assertThat(job.getFirstSeenAt()).isEqualTo(job.getLastSeenAt());

        List<JobSnapshot> snapshots = snapshotRepository.findByJobIdOrderByFetchedAtDesc(job.getId());
        assertThat(snapshots).hasSize(1);
        JobSnapshot snapshot = snapshots.get(0);
        assertThat(snapshot.getRawCompany()).isEqualTo("㈜바바리퍼블리카");
        assertThat(snapshot.getRawExperience()).isEqualTo("경력 3년 이상");
        assertThat(snapshot.getRawPayload()).contains("jobkorea");
        assertThat(snapshot.getContentHash()).isNotBlank();
        assertThat(snapshot.getJobId()).isEqualTo(job.getId());
    }

    @Test
    @DisplayName("the same opening on two boards resolves to one job with two source rows")
    void mergesTheSameOpeningAcrossTwoBoards() {
        importLines("jobkorea", JOBKOREA_LINE);
        IngestionOutcome second = importLines("saramin", SARAMIN_LINE);

        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(second.duplicates()).isEqualTo(1);
        assertThat(second.newJobs()).isZero();

        Job job = jobRepository.findAll().get(0);
        List<JobSource> sources = jobSourceRepository.findByJobId(job.getId());
        assertThat(sources).hasSize(2);
        assertThat(job.getSourceCount()).isEqualTo(2);
        assertThat(sources).anySatisfy(source ->
                assertThat(source.getMatchMethod()).isEqualTo(JobSource.MatchMethod.CANONICAL_URL));
        assertThat(sources).anySatisfy(source ->
                assertThat(source.getMatchEvidence()).contains("canonical_url"));
    }

    @Test
    @DisplayName("two renderings of one employer resolve to a single company")
    void resolvesOneCompanyAcrossLegalFormVariants() {
        importLines("jobkorea", JOBKOREA_LINE);
        importLines("saramin", SARAMIN_LINE);

        assertThat(companyRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("re-importing unchanged content adds a sighting but no second snapshot")
    void reimportDoesNotDuplicateSnapshots() {
        importLines("jobkorea", JOBKOREA_LINE);
        IngestionOutcome second = importLines("jobkorea", JOBKOREA_LINE);

        Job job = jobRepository.findAll().get(0);
        assertThat(second.updatedJobs()).isEqualTo(1);
        assertThat(second.newJobs()).isZero();
        assertThat(snapshotRepository.findByJobIdOrderByFetchedAtDesc(job.getId())).hasSize(1);
        assertThat(sightingRepository.countByJobId(job.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("a later observation never moves first_seen_at")
    void preservesFirstSeenAt() {
        importLines("jobkorea", JOBKOREA_LINE);
        java.time.Instant firstSeen = jobRepository.findAll().get(0).getFirstSeenAt();

        importLines("jobkorea", JOBKOREA_LINE.replace(
                "\"fetchedAt\":\"2026-09-03T00:00:00Z\"", "\"fetchedAt\":\"2026-09-10T00:00:00Z\""));

        Job job = jobRepository.findAll().get(0);
        assertThat(job.getFirstSeenAt()).isEqualTo(firstSeen);
        assertThat(job.getLastSeenAt()).isAfter(firstSeen);
    }

    @Test
    @DisplayName("the far-future sentinel is stored as open-ended rather than as a deadline")
    void storesSentinelDeadlineAsOpenEnded() {
        importLines("jobkorea", OPEN_ENDED_LINE);

        Job job = jobRepository.findAll().get(0);
        assertThat(job.isDeadlineOpenEnded()).isTrue();
        assertThat(job.getDeadlineAt()).isNull();
    }

    @Test
    @DisplayName("a malformed line is persisted with its payload instead of being dropped")
    void persistsMalformedLines() {
        IngestionOutcome outcome = importLines("jobkorea",
                JOBKOREA_LINE, "{not valid json", OPEN_ENDED_LINE);

        assertThat(outcome.status()).isEqualTo(SearchRun.Status.PARTIAL);
        assertThat(outcome.failures()).isEqualTo(1);
        assertThat(outcome.newJobs()).isEqualTo(2);

        List<IngestionFailure> failures =
                failureRepository.findBySearchRunIdOrderByOccurredAtAsc(outcome.searchRunId());
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getStage()).isEqualTo(IngestionFailure.Stage.PARSE);
        assertThat(failures.get(0).getReasonCode()).isEqualTo("MALFORMED_JSON");
        assertThat(failures.get(0).getRawLine()).isEqualTo("{not valid json");
    }

    @Test
    @DisplayName("a line declaring a different source is rejected rather than silently reassigned")
    void rejectsSourceCodeMismatch() {
        IngestionOutcome outcome = importLines("saramin", JOBKOREA_LINE);

        assertThat(outcome.failures()).isEqualTo(1);
        assertThat(jobRepository.findAll()).isEmpty();
        assertThat(failureRepository.findBySearchRunIdOrderByOccurredAtAsc(outcome.searchRunId()))
                .singleElement()
                .satisfies(failure ->
                        assertThat(failure.getReasonCode()).isEqualTo("SOURCE_CODE_MISMATCH"));
    }

    @Test
    @DisplayName("a record missing a required field is recorded as a normalization failure")
    void recordsMissingRequiredFields() {
        String missingCompany = """
                {"sourceCode":"jobkorea","externalId":"1","rawTitle":"Backend Engineer",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawPayload":{}}""";

        IngestionOutcome outcome = importLines("jobkorea", missingCompany);

        assertThat(outcome.failures()).isEqualTo(1);
        assertThat(jobRepository.findAll()).isEmpty();
        assertThat(failureRepository.findBySearchRunIdOrderByOccurredAtAsc(outcome.searchRunId()))
                .singleElement()
                .satisfies(failure -> {
                    assertThat(failure.getStage()).isEqualTo(IngestionFailure.Stage.NORMALIZE);
                    assertThat(failure.getReasonCode()).isEqualTo("MISSING_REQUIRED_FIELD");
                });
    }

    @Test
    @DisplayName("every import is recorded as a run with counters that add up")
    void recordsRunCounters() {
        IngestionOutcome outcome = importLines("jobkorea", JOBKOREA_LINE, OPEN_ENDED_LINE);

        SearchRun run = searchRunRepository.findById(outcome.searchRunId()).orElseThrow();
        assertThat(run.getTriggerKind()).isEqualTo(SearchRun.TriggerKind.IMPORT);
        assertThat(run.getCollector()).isEqualTo("integration-test");
        assertThat(run.getRecordsReceived()).isEqualTo(2);
        assertThat(run.getRecordsNormalized()).isEqualTo(2);
        assertThat(run.getNewJobs()).isEqualTo(2);
        assertThat(run.getCompletedAt()).isNotNull();
        assertThat(run.getDurationMs()).isNotNull();
        assertThat(run.getRunUuid()).isNotNull();
    }

    private IngestionOutcome importLines(String sourceCode, String... lines) {
        String body = String.join("\n", lines);
        return importService.importNdjson(sourceCode,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                "integration-test");
    }
}
