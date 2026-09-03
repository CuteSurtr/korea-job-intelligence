package com.kji.web;

import com.kji.intelligence.JobIntelligence;
import com.kji.intelligence.JobIntelligenceField;
import com.kji.intelligence.JobIntelligenceFieldRepository;
import com.kji.intelligence.JobIntelligenceRepository;
import com.kji.intelligence.JobSkill;
import com.kji.intelligence.JobSkillRepository;
import com.kji.intelligence.Skill;
import com.kji.intelligence.SkillRepository;
import com.kji.job.Job;
import com.kji.job.JobLifecycleEventRepository;
import com.kji.job.JobRepository;
import com.kji.job.JobSourceRepository;
import com.kji.job.JobVerificationRepository;
import com.kji.scoring.CandidateProfile;
import com.kji.scoring.CandidateProfileRepository;
import com.kji.scoring.JobScoreRepository;
import com.kji.snapshot.JobSnapshot;
import com.kji.snapshot.JobSnapshotRepository;
import com.kji.source.Source;
import com.kji.source.SourceRepository;
import com.kji.web.dto.JobDetailResponse;
import com.kji.web.dto.JobIntelligenceResponse;
import com.kji.web.dto.JobResponse;
import com.kji.web.dto.JobScoreResponse;
import com.kji.web.dto.JobSourceResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobDetailAssembler {

    private static final int MAX_SNAPSHOTS = 25;
    private static final int MAX_VERIFICATIONS = 25;

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSnapshotRepository snapshotRepository;
    private final JobVerificationRepository verificationRepository;
    private final JobLifecycleEventRepository lifecycleEventRepository;
    private final JobIntelligenceRepository intelligenceRepository;
    private final JobIntelligenceFieldRepository intelligenceFieldRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final JobScoreRepository scoreRepository;
    private final CandidateProfileRepository profileRepository;
    private final SourceRepository sourceRepository;

    public JobDetailAssembler(JobRepository jobRepository,
                              JobSourceRepository jobSourceRepository,
                              JobSnapshotRepository snapshotRepository,
                              JobVerificationRepository verificationRepository,
                              JobLifecycleEventRepository lifecycleEventRepository,
                              JobIntelligenceRepository intelligenceRepository,
                              JobIntelligenceFieldRepository intelligenceFieldRepository,
                              JobSkillRepository jobSkillRepository,
                              SkillRepository skillRepository,
                              JobScoreRepository scoreRepository,
                              CandidateProfileRepository profileRepository,
                              SourceRepository sourceRepository) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.snapshotRepository = snapshotRepository;
        this.verificationRepository = verificationRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.intelligenceRepository = intelligenceRepository;
        this.intelligenceFieldRepository = intelligenceFieldRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.skillRepository = skillRepository;
        this.scoreRepository = scoreRepository;
        this.profileRepository = profileRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public JobDetailResponse assemble(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No job with id " + jobId));

        Map<Long, String> sourceCodes = sourceCodesById();

        List<JobSourceResponse> sources = jobSourceRepository.findByJobId(jobId).stream()
                .map(jobSource -> JobSourceResponse.from(jobSource,
                        sourceCodes.getOrDefault(jobSource.getSourceId(), "unknown")))
                .toList();

        List<JobDetailResponse.SnapshotSummary> snapshots =
                snapshotRepository.findByJobIdOrderByFetchedAtDesc(jobId).stream()
                        .limit(MAX_SNAPSHOTS)
                        .map(snapshot -> toSnapshotSummary(snapshot, sourceCodes))
                        .toList();

        List<JobDetailResponse.VerificationSummary> verifications =
                verificationRepository.findByJobIdOrderByVerifiedAtDesc(
                                jobId, PageRequest.of(0, MAX_VERIFICATIONS)).stream()
                        .map(verification -> new JobDetailResponse.VerificationSummary(
                                verification.getId(),
                                verification.getVerifiedAt(),
                                verification.getMethod().name(),
                                verification.getOutcome().name(),
                                verification.getHttpStatus(),
                                verification.getSnapshotId(),
                                verification.getDetail()))
                        .toList();

        List<JobDetailResponse.LifecycleEventSummary> lifecycle =
                lifecycleEventRepository.findByJobIdOrderByOccurredAtDesc(jobId).stream()
                        .map(event -> new JobDetailResponse.LifecycleEventSummary(
                                event.getId(),
                                event.getFromState() == null ? null : event.getFromState().name(),
                                event.getToState().name(),
                                event.getReasonCode(),
                                event.getOccurredAt(),
                                event.getVerificationId()))
                        .toList();

        return new JobDetailResponse(JobResponse.from(job), job.getDescription(),
                intelligenceFor(jobId), scoresFor(jobId), sources, snapshots, verifications,
                lifecycle);
    }

    private JobIntelligenceResponse intelligenceFor(Long jobId) {
        JobIntelligence intelligence = intelligenceRepository.findByJobId(jobId).orElse(null);
        if (intelligence == null) {
            return null;
        }
        List<JobIntelligenceResponse.FieldEvidence> fields =
                intelligenceFieldRepository.findByJobId(jobId).stream()
                        .map(this::toFieldEvidence)
                        .toList();

        Map<String, String> skillCategories = new HashMap<>();
        for (Skill skill : skillRepository.findAll()) {
            skillCategories.put(skill.getSlug(), skill.getCategory().name());
        }
        List<JobIntelligenceResponse.SkillEvidence> skills =
                jobSkillRepository.findByJobId(jobId).stream()
                        .map(jobSkill -> toSkillEvidence(jobSkill, skillCategories))
                        .toList();

        return new JobIntelligenceResponse(
                intelligence.getExtractorVersion(),
                intelligence.getRoleFamily(),
                intelligence.getSeniorityBucket(),
                intelligence.getSeniorityLabel(),
                intelligence.getYearsExperienceMin(),
                intelligence.getYearsExperienceMax(),
                intelligence.getDegreeRequired(),
                intelligence.getDegreePreferred(),
                intelligence.getEmploymentType(),
                intelligence.getRemotePolicy(),
                intelligence.getSalaryMin(),
                intelligence.getSalaryMax(),
                intelligence.getSalaryCurrency(),
                intelligence.getSalaryPeriod(),
                Arrays.asList(intelligence.getResponsibilities()),
                Arrays.asList(intelligence.getRequirements()),
                Arrays.asList(intelligence.getPreferredRequirements()),
                intelligence.getExtractedAt(),
                fields,
                skills);
    }

    private List<JobScoreResponse> scoresFor(Long jobId) {
        Map<Long, String> profileCodes = new HashMap<>();
        for (CandidateProfile profile : profileRepository.findAll()) {
            profileCodes.put(profile.getId(), profile.getCode());
        }
        return scoreRepository.findByJobId(jobId).stream()
                .map(score -> JobScoreResponse.from(score,
                        score.getProfileId() == null
                                ? null
                                : profileCodes.get(score.getProfileId())))
                .toList();
    }

    private JobIntelligenceResponse.FieldEvidence toFieldEvidence(JobIntelligenceField field) {
        return new JobIntelligenceResponse.FieldEvidence(
                field.getFieldName(),
                field.getFieldValue(),
                field.getConfidence(),
                field.getExtractionMethod().name(),
                field.getEvidenceText(),
                field.getEvidenceSnapshotId(),
                field.getExtractedAt());
    }

    private JobIntelligenceResponse.SkillEvidence toSkillEvidence(JobSkill jobSkill,
                                                                  Map<String, String> categories) {
        return new JobIntelligenceResponse.SkillEvidence(
                jobSkill.getSkillSlug(),
                categories.getOrDefault(jobSkill.getSkillSlug(), "TOOL"),
                jobSkill.getRequirementLevel().name(),
                jobSkill.getConfidence(),
                jobSkill.getEvidenceText(),
                jobSkill.getEvidenceSnapshotId());
    }

    private Map<Long, String> sourceCodesById() {
        Map<Long, String> codes = new HashMap<>();
        for (Source source : sourceRepository.findAll()) {
            codes.put(source.getId(), source.getCode());
        }
        return codes;
    }

    private JobDetailResponse.SnapshotSummary toSnapshotSummary(JobSnapshot snapshot,
                                                                Map<Long, String> sourceCodes) {
        return new JobDetailResponse.SnapshotSummary(
                snapshot.getId(),
                sourceCodes.getOrDefault(snapshot.getSourceId(), "unknown"),
                snapshot.getExternalId(),
                snapshot.getSourceUrl(),
                snapshot.getFetchedAt(),
                snapshot.getContentHash(),
                snapshot.getRawTitle(),
                snapshot.getRawCompany(),
                snapshot.getRawLocation(),
                snapshot.getRawExperience(),
                snapshot.getRawEducation(),
                snapshot.getRawDeadline());
    }
}
