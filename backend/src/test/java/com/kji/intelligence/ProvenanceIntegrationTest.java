package com.kji.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kji.ingest.ImportService;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.scoring.JobScore;
import com.kji.scoring.JobScoreRepository;
import com.kji.snapshot.JobSnapshot;
import com.kji.snapshot.JobSnapshotRepository;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProvenanceIntegrationTest extends AbstractIntegrationTest {

    private static final String THREE_YEARS = """
            {"sourceCode":"jobkorea","externalId":"49705033",\
            "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49705033",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"[토스] Network Engineer (Datacenter)",\
            "rawCompany":"㈜바바리퍼블리카","rawLocation":"서울 서초구",\
            "rawExperience":"경력 3년 이상","rawEducation":"학력무관",\
            "rawDeadline":"2026. 10. 02.","rawPayload":{"board":"jobkorea"}}""";

    private static final String IMPLAUSIBLE_YEARS = """
            {"sourceCode":"jobkorea","externalId":"49822576",\
            "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49822576",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"[토스] 사내카페 Barista",\
            "rawCompany":"㈜바바리퍼블리카","rawLocation":"서울 강남구",\
            "rawExperience":"경력 100년 이상","rawEducation":"학력무관",\
            "rawDeadline":"2026. 10. 19.","rawPayload":{"board":"jobkorea"}}""";

    private static final String BACKEND_POSTING = """
            {"sourceCode":"ashby","externalId":"vessl-ai:8673f35a",\
            "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/8673f35a",\
            "originalApplyUrl":"https://jobs.ashbyhq.com/vessl-ai/8673f35a/application",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Junior)",\
            "rawCompany":"VESSL AI","rawLocation":"Seoul","rawEmploymentType":"FullTime",\
            "rawRemotePolicy":"Hybrid","rawDescription":"ABOUT THE ROLE\\nWe build a GPU cloud platform.\\n\
            REQUIREMENTS\\nStrong Java and Spring Boot experience.\\nComfortable with PostgreSQL and Redis.\\n\
            Experience running services on Kubernetes and Docker in production.\\n\
            Familiarity with CI/CD pipelines and automated testing.\\n\
            PREFERRED QUALIFICATIONS\\nExposure to Prometheus and Grafana.\\nInterest in distributed systems.",\
            "rawPayload":{"board":"vessl-ai"}}""";

    @Autowired
    private ImportService importService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobIntelligenceRepository intelligenceRepository;

    @Autowired
    private JobIntelligenceFieldRepository fieldRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JobScoreRepository scoreRepository;

    @Autowired
    private JobSnapshotRepository snapshotRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("a stated experience requirement traces back to the phrase in its source snapshot")
    void experienceRequirementIsTraceableToItsSnapshot() {
        importLines("jobkorea", THREE_YEARS);

        Job job = jobRepository.findAll().get(0);
        JobIntelligence intelligence = intelligenceRepository.findByJobId(job.getId()).orElseThrow();
        assertThat(intelligence.getYearsExperienceMin()).isEqualTo(3);

        JobIntelligenceField field = fieldRepository.findByJobIdAndFieldNameAndExtractorVersion(
                job.getId(), "years_experience_min", IntelligenceExtractor.EXTRACTOR_VERSION)
                .orElseThrow();
        assertThat(field.getFieldValue()).isEqualTo("3");
        assertThat(field.getConfidence().doubleValue()).isGreaterThan(0.8d);
        assertThat(field.getExtractionMethod())
                .isEqualTo(JobIntelligenceField.ExtractionMethod.PATTERN_MATCH);
        assertThat(field.getEvidenceText()).contains("3");
        assertThat(field.getEvidenceSnapshotId()).isNotNull();

        JobSnapshot snapshot = snapshotRepository.findById(field.getEvidenceSnapshotId()).orElseThrow();
        assertThat(snapshot.getRawExperience()).isEqualTo("경력 3년 이상");
        assertThat(snapshot.getSourceUrl()).contains("jobkorea.co.kr");
    }

    @Test
    @DisplayName("an implausible source value produces no claim at all, not a wrong one")
    void implausibleValueProducesNoClaim() {
        importLines("jobkorea", IMPLAUSIBLE_YEARS);

        Job job = jobRepository.findAll().get(0);
        JobIntelligence intelligence = intelligenceRepository.findByJobId(job.getId()).orElseThrow();

        assertThat(intelligence.getYearsExperienceMin()).isNull();
        assertThat(intelligence.getYearsExperienceMax()).isNull();
        assertThat(fieldRepository.findByJobIdAndFieldNameAndExtractorVersion(
                job.getId(), "years_experience_min", IntelligenceExtractor.EXTRACTOR_VERSION))
                .isEmpty();

        JobSnapshot snapshot = snapshotRepository
                .findByJobIdOrderByFetchedAtDesc(job.getId()).get(0);
        assertThat(snapshot.getRawExperience()).isEqualTo("경력 100년 이상");
    }

    @Test
    @DisplayName("skills are extracted with the section that stated them and the evidence phrase")
    void skillsCarryRequirementLevelAndEvidence() {
        importLines("ashby", BACKEND_POSTING);

        Job job = jobRepository.findAll().get(0);
        List<JobSkill> skills = jobSkillRepository.findByJobId(job.getId());

        assertThat(skills).isNotEmpty();
        assertThat(skills).extracting(JobSkill::getSkillSlug)
                .contains("java", "spring-boot", "postgresql", "redis", "kubernetes", "docker");

        JobSkill java = skills.stream()
                .filter(skill -> skill.getSkillSlug().equals("java"))
                .findFirst()
                .orElseThrow();
        assertThat(java.getRequirementLevel()).isEqualTo(JobSkill.RequirementLevel.REQUIRED);
        assertThat(java.getEvidenceText()).isNotBlank();
        assertThat(java.getEvidenceSnapshotId()).isNotNull();

        Optional<JobSkill> prometheus = skills.stream()
                .filter(skill -> skill.getSkillSlug().equals("prometheus"))
                .findFirst();
        assertThat(prometheus).isPresent();
        assertThat(prometheus.get().getRequirementLevel())
                .isEqualTo(JobSkill.RequirementLevel.PREFERRED);
    }

    @Test
    @DisplayName("career value and candidate fit are stored as separate, explained scores")
    void scoresAreSeparateAndExplained() {
        importLines("ashby", BACKEND_POSTING);

        Job job = jobRepository.findAll().get(0);
        List<JobScore> scores = scoreRepository.findByJobId(job.getId());

        assertThat(scores).extracting(JobScore::getScoreKind)
                .contains(JobScore.Kind.CAREER_VALUE, JobScore.Kind.CANDIDATE_FIT,
                        JobScore.Kind.APPLICATION_PRIORITY);

        JobScore careerValue = scoreOf(scores, JobScore.Kind.CAREER_VALUE);
        assertThat(careerValue.getProfileId()).isNull();
        assertThat(careerValue.getScore().doubleValue()).isGreaterThan(40.0d);
        assertThat(careerValue.getComponentScores()).contains("components");
        assertThat(careerValue.getExplanation()).isNotBlank();

        JobScore fit = scoreOf(scores, JobScore.Kind.CANDIDATE_FIT);
        assertThat(fit.getProfileId()).isNotNull();
        assertThat(fit.getExplanation()).isNotBlank();
    }

    @Test
    @DisplayName("a clerical posting scores low on career value while staying accessible on fit")
    void clericalPostingSeparatesTheTwoScores() {
        importLines("jobkorea", """
                {"sourceCode":"jobkorea","externalId":"49835112",\
                "sourceUrl":"https://www.jobkorea.co.kr/Recruit/GI_Read/49835112",\
                "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"[토스] Recruiting Assistant",\
                "rawCompany":"㈜바바리퍼블리카","rawLocation":"서울 강남구","rawExperience":"신입",\
                "rawDescription":"Support the recruiting team with interview scheduling and general affairs.",\
                "rawPayload":{}}""");

        Job job = jobRepository.findAll().get(0);
        List<JobScore> scores = scoreRepository.findByJobId(job.getId());

        JobScore careerValue = scoreOf(scores, JobScore.Kind.CAREER_VALUE);
        JobScore fit = scoreOf(scores, JobScore.Kind.CANDIDATE_FIT);

        assertThat(careerValue.getScore().doubleValue()).isLessThan(15.0d);
        assertThat(fit.getScore().doubleValue()).isGreaterThan(careerValue.getScore().doubleValue());
        assertThat(careerValue.getExplanation()).isNotBlank();
    }

    @Test
    @DisplayName("re-importing recomputes intelligence in place rather than accumulating rows")
    void reimportRecomputesInPlace() {
        importLines("ashby", BACKEND_POSTING);
        importLines("ashby", BACKEND_POSTING);

        Job job = jobRepository.findAll().get(0);
        assertThat(intelligenceRepository.findAll()).hasSize(1);
        assertThat(fieldRepository.findByJobId(job.getId()))
                .extracting(JobIntelligenceField::getFieldName)
                .doesNotHaveDuplicates();
        assertThat(jobSkillRepository.findByJobId(job.getId()))
                .extracting(JobSkill::getSkillSlug)
                .doesNotHaveDuplicates();
    }

    private JobScore scoreOf(List<JobScore> scores, JobScore.Kind kind) {
        return scores.stream()
                .filter(score -> score.getScoreKind() == kind)
                .findFirst()
                .orElseThrow();
    }

    private void importLines(String sourceCode, String... lines) {
        String body = String.join("\n", lines);
        importService.importNdjson(sourceCode,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), "integration-test");
    }
}
