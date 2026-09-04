package com.kji.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.job.JobRepository;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ApiIntegrationTest extends AbstractIntegrationTest {

    private static final String NDJSON = """
            {"sourceCode":"ashby","externalId":"vessl-ai:junior",\
            "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/junior",\
            "originalApplyUrl":"https://jobs.ashbyhq.com/vessl-ai/junior/application",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Junior)",\
            "rawCompany":"VESSL AI","rawLocation":"Seoul","rawEmploymentType":"FullTime",\
            "rawRemotePolicy":"Hybrid","rawDescription":"REQUIREMENTS\\nJava and Spring Boot in production.\\n\
            PostgreSQL, Redis, Docker and Kubernetes.\\nPREFERRED QUALIFICATIONS\\nPrometheus and Grafana.",\
            "rawPayload":{"board":"vessl-ai"}}
            {"sourceCode":"ashby","externalId":"vessl-ai:senior",\
            "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/senior",\
            "originalApplyUrl":"https://jobs.ashbyhq.com/vessl-ai/senior/application",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Senior)",\
            "rawCompany":"VESSL AI","rawLocation":"Seoul","rawEmploymentType":"FullTime",\
            "rawDescription":"REQUIREMENTS\\nSenior engineer with 7 years of experience in Go and Kubernetes.",\
            "rawPayload":{"board":"vessl-ai"}}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void seed() throws Exception {
        databaseCleaner.clean();
        mockMvc.perform(post("/api/internal/ingestion/import")
                        .param("source", "ashby")
                        .param("collector", "api-test")
                        .header("X-Internal-Token", "test-token")
                        .contentType("application/x-ndjson")
                        .content(NDJSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the internal ingestion endpoint refuses a request without the shared token")
    void internalEndpointRequiresToken() throws Exception {
        mockMvc.perform(post("/api/internal/ingestion/import")
                        .param("source", "ashby")
                        .contentType("application/x-ndjson")
                        .content(NDJSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the job list exposes the fields the jobs table needs to compare postings")
    void jobListExposesComparableFields() throws Exception {
        String body = mockMvc.perform(get("/api/jobs").param("sort", "NEWEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode first = objectMapper.readTree(body).get("content").get(0);
        assertThat(first.has("companyName")).isTrue();
        assertThat(first.has("roleFamily")).isTrue();
        assertThat(first.has("seniorityBucket")).isTrue();
        assertThat(first.has("careerValueScore")).isTrue();
        assertThat(first.has("candidateFitScore")).isTrue();
        assertThat(first.has("sourceCount")).isTrue();
        assertThat(first.has("lastVerifiedAt")).isTrue();
    }

    @Test
    @DisplayName("a junior and a senior posting at one company stay two rows")
    void juniorAndSeniorDoNotMerge() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("filtering by seniority bucket narrows the list")
    void filtersBySeniority() throws Exception {
        mockMvc.perform(get("/api/jobs").param("seniority", "X"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer (Senior)"));
    }

    @Test
    @DisplayName("job detail returns the evidence chain alongside the canonical posting")
    void jobDetailReturnsEvidence() throws Exception {
        Long jobId = jobRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.id").value(jobId))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.snapshots").isArray())
                .andExpect(jsonPath("$.verifications").isArray())
                .andExpect(jsonPath("$.lifecycle").isArray())
                .andExpect(jsonPath("$.intelligence.extractorVersion").isNotEmpty())
                .andExpect(jsonPath("$.scores").isArray());
    }

    @Test
    @DisplayName("an unknown job id is a 404 rather than a 500")
    void unknownJobIsNotFound() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    @DisplayName("an unknown sort order is rejected as a bad request")
    void unknownSortIsRejected() throws Exception {
        mockMvc.perform(get("/api/jobs").param("sort", "MAGIC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    @DisplayName("the source registry reports which providers the backend can query itself")
    void sourceRegistryReportsRuntimeAvailability() throws Exception {
        String body = mockMvc.perform(get("/api/sources"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode sources = objectMapper.readTree(body);
        JsonNode greenhouse = find(sources, "greenhouse");
        JsonNode jobkorea = find(sources, "jobkorea");

        assertThat(greenhouse.get("runtimeAvailable").asBoolean()).isTrue();
        assertThat(greenhouse.get("adapterRegistered").asBoolean()).isTrue();
        assertThat(jobkorea.get("runtimeAvailable").asBoolean()).isFalse();
        assertThat(jobkorea.get("adapterRegistered").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("source health is exposed with latency, failures and circuit state")
    void sourceHealthIsExposed() throws Exception {
        mockMvc.perform(get("/api/sources/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceCode").isNotEmpty())
                .andExpect(jsonPath("$[0].circuitState").isNotEmpty());
    }

    @Test
    @DisplayName("search runs list their counters and a run detail lists its failures")
    void searchRunsAreQueryable() throws Exception {
        String body = mockMvc.perform(get("/api/search-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sourceCode").value("ashby"))
                .andExpect(jsonPath("$.content[0].collector").value("api-test"))
                .andReturn().getResponse().getContentAsString();

        long runId = objectMapper.readTree(body).get("content").get(0).get("id").asLong();
        mockMvc.perform(get("/api/search-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureDetails").isArray());
    }

    @Test
    @DisplayName("an application is created, transitioned and keeps its full status history")
    void applicationLifecycleIsTracked() throws Exception {
        Long jobId = jobRepository.findAll().get(0).getId();

        String created = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobId":%d,"status":"INTERESTED","note":"Looks like a fit"}
                                """.formatted(jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERESTED"))
                .andExpect(jsonPath("$.profileCode").value("default"))
                .andReturn().getResponse().getContentAsString();

        long applicationId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(patch("/api/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobId":%d,"status":"APPLIED","resumeVersion":"v3",
                                 "note":"Submitted through the ATS"}
                                """.formatted(jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty())
                .andExpect(jsonPath("$.resumeVersion").value("v3"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[1].fromStatus").value("INTERESTED"))
                .andExpect(jsonPath("$.history[1].toStatus").value("APPLIED"));

        mockMvc.perform(get("/api/applications").param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("the application list answers whether one job is already tracked")
    void applicationsCanBeLookedUpByJob() throws Exception {
        Long tracked = jobRepository.findAll().get(0).getId();
        Long untracked = jobRepository.findAll().get(1).getId();

        // The console asks this before showing a job, to decide between "track" and "open".
        mockMvc.perform(get("/api/applications").param("jobId", String.valueOf(tracked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobId":%d,"status":"INTERESTED"}
                                """.formatted(tracked)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/applications").param("jobId", String.valueOf(tracked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].jobId").value(tracked))
                // the lookup carries the history, so the job page can show the last change
                .andExpect(jsonPath("$.content[0].history.length()").value(1));

        mockMvc.perform(get("/api/applications").param("jobId", String.valueOf(untracked)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // the status filter still narrows a job lookup
        mockMvc.perform(get("/api/applications")
                        .param("jobId", String.valueOf(tracked))
                        .param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("posting the same job twice updates the application rather than duplicating it")
    void repeatedCreateUpdatesInPlace() throws Exception {
        Long jobId = jobRepository.findAll().get(0).getId();
        String body = """
                {"jobId":%d,"status":"%s"}
                """;

        String first = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(jobId, "INTERESTED")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(jobId, "READY_TO_APPLY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_APPLY"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second).get("id").asLong())
                .isEqualTo(objectMapper.readTree(first).get("id").asLong());

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("an unknown application status is refused with the statuses that would work")
    void unknownStatusIsRefused() throws Exception {
        Long jobId = jobRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobId":%d,"status":"MAYBE_LATER"}
                                """.formatted(jobId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("MAYBE_LATER"),
                        org.hamcrest.Matchers.containsString("READY_TO_APPLY"))));
    }

    @Test
    @DisplayName("the dashboard reports lifecycle counts, source health and recent runs")
    void dashboardAggregates() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(2))
                .andExpect(jsonPath("$.jobsByLifecycleState.ACTIVE").value(2))
                .andExpect(jsonPath("$.profileCode").value("default"))
                .andExpect(jsonPath("$.recentRuns[0].sourceCode").value("ashby"))
                .andExpect(jsonPath("$.applicationsByStatus.NOT_REVIEWED").exists());
    }

    @Test
    @DisplayName("companies expose their aliases and provider identifiers")
    void companyDetailExposesIdentity() throws Exception {
        String body = mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long companyId = objectMapper.readTree(body).get(0).get("id").asLong();
        mockMvc.perform(get("/api/companies/{id}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canonicalName").value("VESSL AI"))
                .andExpect(jsonPath("$.openJobCount").value(2))
                .andExpect(jsonPath("$.identifiers").isArray())
                .andExpect(jsonPath("$.riskLevel").value("UNKNOWN"));
    }

    private JsonNode find(JsonNode sources, String code) {
        for (JsonNode source : sources) {
            if (code.equals(source.get("code").asText())) {
                return source;
            }
        }
        throw new IllegalStateException("No source " + code);
    }
}
