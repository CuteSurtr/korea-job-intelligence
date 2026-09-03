package com.kji.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kji.ingest.IngestionFailure;
import com.kji.ingest.IngestionFailureRepository;
import com.kji.ingest.IngestionOutcome;
import com.kji.ingest.IngestionPipeline;
import com.kji.ingest.SearchRun;
import com.kji.source.SourceException;
import com.kji.source.SourceHealth;
import com.kji.source.SourceHealthRepository;
import com.kji.source.SourceQuery;
import com.kji.source.SourceRepository;
import com.kji.source.http.HttpJsonClient;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

class JobLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SourceQuery FULL_BOARD =
            new SourceQuery(null, Map.of("board", "coupang"), 200);

    @MockBean
    private HttpJsonClient httpClient;

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private JobVerificationRepository verificationRepository;

    @Autowired
    private JobLifecycleEventRepository lifecycleEventRepository;

    @Autowired
    private IngestionFailureRepository failureRepository;

    @Autowired
    private SourceHealthRepository sourceHealthRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("a source outage leaves every job exactly where it was")
    void sourceOutageClosesNothing() throws IOException {
        givenBoardResponse(fullBoard());
        IngestionOutcome first = pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);
        assertThat(first.newJobs()).isGreaterThan(0);
        List<LifecycleState> before = lifecycleStates();

        given(httpClient.getJson(any(URI.class)))
                .willThrow(new SourceException("Connection reset", 503, null));
        IngestionOutcome outage = pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        assertThat(outage.status()).isEqualTo(SearchRun.Status.FAILED);
        assertThat(outage.jobsClosed()).isZero();
        assertThat(lifecycleStates()).isEqualTo(before);
        assertThat(jobRepository.findAll())
                .allSatisfy(job -> assertThat(job.getLifecycleState()).isEqualTo(LifecycleState.ACTIVE));
        assertThat(verificationRepository.findAll())
                .noneMatch(verification ->
                        verification.getOutcome() == JobVerification.Outcome.ABSENT);
    }

    @Test
    @DisplayName("a failed fetch is recorded against the run and the source, not against the jobs")
    void sourceOutageIsRecordedOnRunAndHealth() throws IOException {
        givenBoardResponse(fullBoard());
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        given(httpClient.getJson(any(URI.class)))
                .willThrow(new SourceException("Connection reset", 503, null));
        IngestionOutcome outage = pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        List<IngestionFailure> failures =
                failureRepository.findBySearchRunIdOrderByOccurredAtAsc(outage.searchRunId());
        assertThat(failures).singleElement().satisfies(failure -> {
            assertThat(failure.getStage()).isEqualTo(IngestionFailure.Stage.FETCH);
            assertThat(failure.getReasonCode()).isEqualTo("FETCH_FAILED");
        });

        SourceHealth health = healthFor("greenhouse");
        assertThat(health.getConsecutiveFailures()).isEqualTo(1);
        assertThat(health.getLastStatus()).isEqualTo("FETCH_FAILED");
        assertThat(health.getLastFailureAt()).isNotNull();
        assertThat(health.getLastSuccessAt()).isNotNull();
    }

    @Test
    @DisplayName("absence from a successful complete listing closes the job, with the evidence stored")
    void absenceFromCompleteListingClosesJob() throws IOException {
        givenBoardResponse(fullBoard());
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        String removedExternalKey = jobSourceRepository.findAll().get(0).getExternalKey();
        Long removedJobId = jobSourceRepository.findAll().get(0).getJob().getId();

        givenBoardResponse(boardWithout(removedExternalKey));
        IngestionOutcome outcome =
                pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        assertThat(outcome.jobsClosed()).isEqualTo(1);

        Job closed = jobRepository.findById(removedJobId).orElseThrow();
        assertThat(closed.getLifecycleState()).isEqualTo(LifecycleState.CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
        assertThat(closed.getClosedReason()).isEqualTo("ABSENT_FROM_ALL_SOURCES");
        assertThat(closed.getClosedEvidenceId()).isNotNull();

        JobVerification evidence =
                verificationRepository.findById(closed.getClosedEvidenceId()).orElseThrow();
        assertThat(evidence.getMethod()).isEqualTo(JobVerification.Method.SOURCE_LISTING_ABSENT);
        assertThat(evidence.getOutcome()).isEqualTo(JobVerification.Outcome.ABSENT);
        assertThat(evidence.supportsClosure()).isTrue();

        assertThat(jobRepository.findAll())
                .filteredOn(job -> !job.getId().equals(removedJobId))
                .allSatisfy(job -> assertThat(job.getLifecycleState()).isEqualTo(LifecycleState.ACTIVE));
    }

    @Test
    @DisplayName("a posting seen again after closure reopens without losing its first sighting")
    void closedJobReopensOnLaterObservation() throws IOException {
        givenBoardResponse(fullBoard());
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        String removedExternalKey = jobSourceRepository.findAll().get(0).getExternalKey();
        Long jobId = jobSourceRepository.findAll().get(0).getJob().getId();
        Instant originalFirstSeen = jobRepository.findById(jobId).orElseThrow().getFirstSeenAt();

        givenBoardResponse(boardWithout(removedExternalKey));
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);
        assertThat(jobRepository.findById(jobId).orElseThrow().getLifecycleState())
                .isEqualTo(LifecycleState.CLOSED);

        givenBoardResponse(fullBoard());
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        Job reopened = jobRepository.findById(jobId).orElseThrow();
        assertThat(reopened.getLifecycleState()).isEqualTo(LifecycleState.REOPENED);
        assertThat(reopened.getReopenedAt()).isNotNull();
        assertThat(reopened.getClosedAt()).isNotNull();
        assertThat(reopened.getFirstSeenAt()).isEqualTo(originalFirstSeen);

        assertThat(lifecycleEventRepository.findByJobIdOrderByOccurredAtDesc(jobId))
                .extracting(JobLifecycleEvent::getToState)
                .containsSequence(LifecycleState.REOPENED, LifecycleState.CLOSED);
    }

    @Test
    @DisplayName("every observation writes a present verification that advances last_verified_at")
    void observationRecordsPresentVerification() throws IOException {
        givenBoardResponse(fullBoard());
        pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);

        Job job = jobRepository.findAll().get(0);
        assertThat(job.getLastVerifiedAt()).isNotNull();
        assertThat(verificationRepository.findByJobIdOrderByVerifiedAtDesc(
                job.getId(), PageRequest.of(0, 5)))
                .singleElement()
                .satisfies(verification -> {
                    assertThat(verification.getMethod())
                            .isEqualTo(JobVerification.Method.SOURCE_LISTING_PRESENT);
                    assertThat(verification.getOutcome())
                            .isEqualTo(JobVerification.Outcome.PRESENT);
                });
    }

    @Test
    @DisplayName("repeated failures open the circuit and the next run is skipped rather than retried")
    void repeatedFailuresOpenTheCircuit() {
        given(httpClient.getJson(any(URI.class)))
                .willThrow(new SourceException("Connection reset", 503, null));

        for (int attempt = 0; attempt < 5; attempt++) {
            pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);
        }
        assertThat(healthFor("greenhouse").getCircuitState())
                .isEqualTo(SourceHealth.CircuitState.OPEN);

        IngestionOutcome skipped =
                pipeline.runDirect("greenhouse", FULL_BOARD, SearchRun.TriggerKind.MANUAL);
        assertThat(skipped.status()).isEqualTo(SearchRun.Status.SKIPPED);
    }

    @Test
    @DisplayName("an import-only source cannot be run directly")
    void importOnlySourceIsNotRunnable() {
        assertThat(sourceRepository.findByCode("jobkorea").orElseThrow().isRuntimeAvailable())
                .isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        pipeline.runDirect("jobkorea", SourceQuery.of("backend"),
                                SearchRun.TriggerKind.MANUAL))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("not runtime-available");
    }

    private void givenBoardResponse(JsonNode body) {
        given(httpClient.getJson(any(URI.class)))
                .willReturn(new HttpJsonClient.JsonResponse(200, body, 12L));
    }

    private List<LifecycleState> lifecycleStates() {
        return jobRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .map(Job::getLifecycleState)
                .toList();
    }

    private SourceHealth healthFor(String code) {
        Long sourceId = sourceRepository.findByCode(code).orElseThrow().getId();
        return sourceHealthRepository.findBySourceId(sourceId).orElseThrow();
    }

    private JsonNode fullBoard() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("fixtures/greenhouse/coupang-board.json")) {
            return MAPPER.readTree(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private JsonNode boardWithout(String externalKey) throws IOException {
        ObjectNode board = (ObjectNode) fullBoard();
        ArrayNode jobs = (ArrayNode) board.get("jobs");
        ArrayNode kept = MAPPER.createArrayNode();
        for (JsonNode job : jobs) {
            if (!externalKey.equals("coupang:" + job.path("id").asText())) {
                kept.add(job);
            }
        }
        board.set("jobs", kept);
        return board;
    }
}
