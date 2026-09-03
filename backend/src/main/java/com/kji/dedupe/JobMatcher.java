package com.kji.dedupe;

import com.kji.config.DedupeProperties;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobMatcher {

    private static final double CANONICAL_URL_CONFIDENCE = 1.000d;
    private static final double EXTERNAL_ID_CONFIDENCE = 0.980d;
    private static final double COMPANY_TITLE_LOCATION_CONFIDENCE = 0.850d;
    private static final double TITLE_TRIGRAM_THRESHOLD = 0.98d;

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final DedupeProperties properties;

    public JobMatcher(JobRepository jobRepository,
                      JobSourceRepository jobSourceRepository,
                      DedupeProperties properties) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public JobMatch match(JobMatchCandidate candidate) {
        Optional<JobMatch> byUrl = matchByCanonicalUrl(candidate);
        if (byUrl.isPresent()) {
            return byUrl.get();
        }
        Optional<JobMatch> byExternalId = matchByExternalId(candidate);
        if (byExternalId.isPresent()) {
            return byExternalId.get();
        }
        Optional<JobMatch> byTitle = matchByCompanyTitleLocation(candidate);
        if (byTitle.isPresent()) {
            return byTitle.get();
        }
        Optional<JobMatch> byDescription = matchByDescriptionSimilarity(candidate);
        return byDescription.orElseGet(JobMatch::none);
    }

    private Optional<JobMatch> matchByCanonicalUrl(JobMatchCandidate candidate) {
        if (candidate.canonicalUrlKey() == null) {
            return Optional.empty();
        }
        Optional<Job> byJob = jobRepository.findByCanonicalUrlKey(candidate.canonicalUrlKey());
        if (byJob.isPresent()) {
            return Optional.of(new JobMatch(byJob.get().getId(), JobSource.MatchMethod.CANONICAL_URL,
                    CANONICAL_URL_CONFIDENCE, evidence("canonical_url", candidate.canonicalUrlKey())));
        }
        return jobSourceRepository.findFirstByCanonicalUrlKey(candidate.canonicalUrlKey())
                .map(jobSource -> new JobMatch(jobSource.getJob().getId(),
                        JobSource.MatchMethod.CANONICAL_URL, CANONICAL_URL_CONFIDENCE,
                        evidence("canonical_url", candidate.canonicalUrlKey())));
    }

    private Optional<JobMatch> matchByExternalId(JobMatchCandidate candidate) {
        if (!candidate.sourceHasStableExternalId() || candidate.externalKey() == null) {
            return Optional.empty();
        }
        return jobSourceRepository.findFirstByExternalKey(candidate.externalKey())
                .map(jobSource -> new JobMatch(jobSource.getJob().getId(),
                        JobSource.MatchMethod.ATS_EXTERNAL_ID, EXTERNAL_ID_CONFIDENCE,
                        evidence("external_key", candidate.externalKey())));
    }

    private Optional<JobMatch> matchByCompanyTitleLocation(JobMatchCandidate candidate) {
        if (candidate.companyId() == null || candidate.normalizedTitle() == null) {
            return Optional.empty();
        }
        List<Job> exact = jobRepository.findByCompanyIdAndNormalizedTitle(
                candidate.companyId(), candidate.normalizedTitle());
        for (Job job : exact) {
            if (locationCompatible(job.getLocationCity(), candidate.locationCity())) {
                return Optional.of(new JobMatch(job.getId(),
                        JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                        COMPANY_TITLE_LOCATION_CONFIDENCE,
                        evidence("normalized_title", candidate.normalizedTitle())));
            }
        }

        List<Long> nearIdentical = jobRepository.findSimilarTitleJobIds(
                candidate.companyId(), candidate.normalizedTitle(), TITLE_TRIGRAM_THRESHOLD, null, 5);
        for (Long jobId : nearIdentical) {
            Optional<Job> job = jobRepository.findById(jobId);
            if (job.isPresent() && locationCompatible(job.get().getLocationCity(), candidate.locationCity())) {
                return Optional.of(new JobMatch(jobId, JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                        COMPANY_TITLE_LOCATION_CONFIDENCE,
                        evidence("normalized_title_trigram", candidate.normalizedTitle())));
            }
        }
        return Optional.empty();
    }

    private Optional<JobMatch> matchByDescriptionSimilarity(JobMatchCandidate candidate) {
        String description = candidate.normalizedDescription();
        if (candidate.companyId() == null
                || description == null
                || description.length() < properties.minDescriptionLength()) {
            return Optional.empty();
        }
        List<Object[]> rows = jobRepository.findSimilarDescriptionJobs(
                candidate.companyId(), description, properties.minDescriptionLength(),
                properties.descriptionSimilarityThreshold(), 3);
        for (Object[] row : rows) {
            Long jobId = ((Number) row[0]).longValue();
            double score = ((Number) row[1]).doubleValue();
            Optional<Job> job = jobRepository.findById(jobId);
            if (job.isEmpty() || !locationCompatible(job.get().getLocationCity(), candidate.locationCity())) {
                continue;
            }
            double confidence = Math.min(0.90d, Math.max(properties.autoMergeThreshold(), score));
            return Optional.of(new JobMatch(jobId, JobSource.MatchMethod.DESCRIPTION_SIMILARITY,
                    confidence, evidence("description_similarity", String.format(Locale.ROOT, "%.3f", score))));
        }
        return Optional.empty();
    }

    private boolean locationCompatible(String existingCity, String candidateCity) {
        if (existingCity == null || candidateCity == null) {
            return true;
        }
        return existingCity.equalsIgnoreCase(candidateCity);
    }

    private String evidence(String rung, String value) {
        String escaped = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"rung\":\"" + rung + "\",\"value\":\"" + escaped + "\"}";
    }
}
