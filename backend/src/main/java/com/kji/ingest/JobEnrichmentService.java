package com.kji.ingest;

import com.kji.intelligence.IntelligenceExtractor;
import com.kji.intelligence.IntelligenceInput;
import com.kji.intelligence.JobIntelligence;
import com.kji.intelligence.JobSkill;
import com.kji.intelligence.JobSkillRepository;
import com.kji.intelligence.Skill;
import com.kji.intelligence.SkillRepository;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.scoring.ScoreResult;
import com.kji.scoring.ScoringInput;
import com.kji.scoring.ScoringService;
import com.kji.source.RawJobRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobEnrichmentService {

    private final IntelligenceExtractor intelligenceExtractor;
    private final ScoringService scoringService;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final Clock clock;

    public JobEnrichmentService(IntelligenceExtractor intelligenceExtractor,
                                ScoringService scoringService,
                                JobSkillRepository jobSkillRepository,
                                SkillRepository skillRepository,
                                JobRepository jobRepository,
                                Clock clock) {
        this.intelligenceExtractor = intelligenceExtractor;
        this.scoringService = scoringService;
        this.jobSkillRepository = jobSkillRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.clock = clock;
    }

    @Transactional
    public void enrich(Job job, RawJobRecord record, Long snapshotId, Long sourceId) {
        JobIntelligence intelligence = intelligenceExtractor.extract(new IntelligenceInput(
                job.getId(),
                snapshotId,
                sourceId,
                job.getCanonicalTitle(),
                job.getDescription(),
                record.rawExperience(),
                record.rawEducation(),
                record.rawEmploymentType(),
                record.rawRemotePolicy(),
                job.getLocationRaw(),
                job.getLocationCity(),
                job.getLocationRegion(),
                job.getLocationCountry()));

        job.applyIntelligenceSummary(
                intelligence.getRoleFamily(),
                intelligence.getSeniorityBucket(),
                intelligence.getYearsExperienceMin(),
                intelligence.getYearsExperienceMax(),
                intelligence.getRemotePolicy(),
                intelligence.getEmploymentType(),
                intelligence.getDegreeRequired());

        ScoringService.Scores scores = scoringService.score(new ScoringInput(
                job.getId(),
                job.getCanonicalTitle(),
                job.getDescription(),
                intelligence.getRoleFamily(),
                intelligence.getSeniorityBucket(),
                intelligence.getYearsExperienceMin(),
                intelligence.getYearsExperienceMax(),
                intelligence.getDegreeRequired(),
                intelligence.getEmploymentType(),
                intelligence.getRemotePolicy(),
                job.getLocationCity(),
                job.getLocationCountry(),
                job.getDeadlineAt(),
                job.isDeadlineOpenEnded(),
                job.getCompany().getRiskLevel().name(),
                skillSignals(job.getId())));

        job.applyScores(
                round(scores.careerValue()),
                round(scores.candidateFit()),
                round(scores.priority()),
                Instant.now(clock));
        jobRepository.save(job);
    }

    private BigDecimal round(ScoreResult result) {
        return result == null
                ? null
                : BigDecimal.valueOf(result.score()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<ScoringInput.SkillSignal> skillSignals(Long jobId) {
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(jobId);
        if (jobSkills.isEmpty()) {
            return List.of();
        }
        Map<String, String> categories = new HashMap<>();
        for (Skill skill : skillRepository.findAll()) {
            categories.put(skill.getSlug(), skill.getCategory().name());
        }
        return jobSkills.stream()
                .map(jobSkill -> new ScoringInput.SkillSignal(
                        jobSkill.getSkillSlug(),
                        categories.getOrDefault(jobSkill.getSkillSlug(), "TOOL"),
                        jobSkill.getRequirementLevel().name()))
                .toList();
    }
}
