package com.kji.dedupe;

import static org.assertj.core.api.Assertions.assertThat;

import com.kji.company.CompanyRepository;
import com.kji.ingest.ImportService;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobMergeCandidateRepository mergeCandidateRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("two postings the employer's own board numbers separately stay two jobs")
    void sameBoardDistinctIdsNeverMerge() {
        importLines("ashby", """
                {"sourceCode":"ashby","externalId":"toss:1001",\
                "sourceUrl":"https://jobs.ashbyhq.com/toss/1001",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Server Developer",\
                "rawCompany":"Toss","rawLocation":"Seoul","rawPayload":{}}""", """
                {"sourceCode":"ashby","externalId":"toss:1002",\
                "sourceUrl":"https://jobs.ashbyhq.com/toss/1002",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Server Developer",\
                "rawCompany":"Toss","rawLocation":"Seoul","rawPayload":{}}""");

        assertThat(jobRepository.findAll()).hasSize(2);
        assertThat(companyRepository.findAll()).hasSize(1);
        assertThat(jobSourceRepository.findAll())
                .extracting(JobSource::getMatchMethod)
                .containsOnly(JobSource.MatchMethod.NEW_JOB);
    }

    @Test
    @DisplayName("an aggregator pointing at the employer's own posting merges into it")
    void aggregatorMergesIntoEmployerPostingByCanonicalUrl() {
        importLines("ashby", """
                {"sourceCode":"ashby","externalId":"vessl-ai:junior",\
                "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/junior",\
                "originalApplyUrl":"https://jobs.ashbyhq.com/vessl-ai/junior",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Junior)",\
                "rawCompany":"VESSL AI","rawLocation":"Seoul","rawPayload":{}}""");

        importLines("freehire", """
                {"sourceCode":"freehire","externalId":"freehire:vessl-ai:junior",\
                "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/junior?utm_source=freehire.me",\
                "originalApplyUrl":"https://jobs.ashbyhq.com/vessl-ai/junior",\
                "fetchedAt":"2026-09-03T01:00:00Z","rawTitle":"Backend Engineer (Junior)",\
                "rawCompany":"VESSL AI","rawLocation":"Seoul","rawPayload":{}}""");

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);

        Job job = jobs.get(0);
        assertThat(job.getSourceCount()).isEqualTo(2);
        assertThat(jobSourceRepository.findByJobId(job.getId()))
                .extracting(JobSource::getMatchMethod)
                .contains(JobSource.MatchMethod.NEW_JOB, JobSource.MatchMethod.CANONICAL_URL);
    }

    @Test
    @DisplayName("a junior and a senior posting at one company never collapse into one job")
    void seniorityTokensKeepPostingsApart() {
        importLines("ashby", """
                {"sourceCode":"ashby","externalId":"vessl-ai:junior",\
                "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/junior",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Junior)",\
                "rawCompany":"VESSL AI","rawLocation":"Seoul","rawPayload":{}}""", """
                {"sourceCode":"ashby","externalId":"vessl-ai:senior",\
                "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/senior",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Senior)",\
                "rawCompany":"VESSL AI","rawLocation":"Seoul","rawPayload":{}}""");

        assertThat(jobRepository.findAll())
                .hasSize(2)
                .extracting(Job::getCanonicalTitle)
                .containsExactlyInAnyOrder("Backend Engineer (Junior)", "Backend Engineer (Senior)");
    }

    @Test
    @DisplayName("the same opening on two Korean boards becomes one job with two source rows")
    void crossBoardDuplicatesMerge() {
        importLines("jobkorea", """
                {"sourceCode":"jobkorea","externalId":"jobkorea:49801434",\
                "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49801434",\
                "fetchedAt":"2026-09-03T00:00:00Z",\
                "rawTitle":"Merchandiser (계약직) - 커머스",\
                "rawCompany":"㈜당근마켓","rawLocation":"I150","rawExperience":"경력 1년 이상",\
                "rawPayload":{}}""");

        importLines("saramin", """
                {"sourceCode":"saramin","externalId":"saramin:54806683",\
                "sourceUrl":"https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=54806683",\
                "fetchedAt":"2026-09-03T00:10:00Z",\
                "rawTitle":"Merchandiser (계약직) - 커머스",\
                "rawCompany":"(주)당근마켓","rawLocation":"서울 강남구","rawExperience":"경력1년↑",\
                "rawPayload":{}}""");

        assertThat(companyRepository.findAll()).hasSize(1);
        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(jobSourceRepository.findAll()).hasSize(2);
        assertThat(jobSourceRepository.findAll())
                .extracting(JobSource::getMatchMethod)
                .contains(JobSource.MatchMethod.COMPANY_TITLE_LOCATION);
    }

    @Test
    @DisplayName("two listings from one provider stay apart and are queued for review instead")
    void sameProviderNearDuplicatesAreQueuedForReview() {
        importLines("indeed", """
                {"sourceCode":"indeed","sourceUrl":"https://to.indeed.com/aaa111",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Developer",\
                "rawCompany":"Ericsson","rawLocation":"서울","rawPayload":{}}""");
        importLines("indeed", """
                {"sourceCode":"indeed","sourceUrl":"https://to.indeed.com/bbb222",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Developer",\
                "rawCompany":"Ericsson","rawLocation":"서울","rawPayload":{}}""");

        assertThat(jobRepository.findAll()).hasSize(2);
        assertThat(jobSourceRepository.findAll())
                .extracting(JobSource::getMatchMethod)
                .containsOnly(JobSource.MatchMethod.NEW_JOB);

        List<JobMergeCandidate> review = mergeCandidateRepository.findAll();
        assertThat(review).singleElement().satisfies(candidate -> {
            assertThat(candidate.getStatus()).isEqualTo(JobMergeCandidate.Status.PENDING);
            assertThat(candidate.getMatchMethod()).isEqualTo("COMPANY_TITLE_LOCATION");
            assertThat(candidate.getLeftJobId()).isLessThan(candidate.getRightJobId());
        });
    }

    private void importLines(String sourceCode, String... lines) {
        String body = String.join("\n", lines);
        importService.importNdjson(sourceCode,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), "dedupe-test");
    }
}
