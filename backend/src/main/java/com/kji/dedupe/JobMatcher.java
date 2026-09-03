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

        Outcome byTitle = matchByCompanyTitleLocation(candidate);
        if (byTitle.match() != null) {
            return byTitle.match();
        }
        Outcome byDescription = matchByDescriptionSimilarity(candidate);
        if (byDescription.match() != null) {
            return byDescription.match();
        }

        JobMatch.ReviewCandidate review = byTitle.review() != null
                ? byTitle.review()
                : byDescription.review();
        return review == null ? JobMatch.none() : JobMatch.none(review);
    }

    private Optional<JobMatch> matchByCanonicalUrl(JobMatchCandidate candidate) {
        if (candidate.canonicalUrlKey() == null) {
            return Optional.empty();
        }
        Optional<Job> byJob = jobRepository.findByCanonicalUrlKey(candidate.canonicalUrlKey());
        if (byJob.isPresent()) {
            return Optional.of(JobMatch.matched(byJob.get().getId(),
                    JobSource.MatchMethod.CANONICAL_URL, CANONICAL_URL_CONFIDENCE,
                    evidence("canonical_url", candidate.canonicalUrlKey())));
        }
        return jobSourceRepository.findFirstByCanonicalUrlKey(candidate.canonicalUrlKey())
                .map(jobSource -> JobMatch.matched(jobSource.getJob().getId(),
                        JobSource.MatchMethod.CANONICAL_URL, CANONICAL_URL_CONFIDENCE,
                        evidence("canonical_url", candidate.canonicalUrlKey())));
    }

    private Optional<JobMatch> matchByExternalId(JobMatchCandidate candidate) {
        if (!candidate.sourceHasStableExternalId() || candidate.externalKey() == null) {
            return Optional.empty();
        }
        return jobSourceRepository.findFirstByExternalKey(candidate.externalKey())
                .map(jobSource -> JobMatch.matched(jobSource.getJob().getId(),
                        JobSource.MatchMethod.ATS_EXTERNAL_ID, EXTERNAL_ID_CONFIDENCE,
                        evidence("external_key", candidate.externalKey())));
    }

    private Outcome matchByCompanyTitleLocation(JobMatchCandidate candidate) {
        if (candidate.companyId() == null || candidate.normalizedTitle() == null) {
            return Outcome.nothing();
        }
        JobMatch.ReviewCandidate review = null;

        List<Job> exact = jobRepository.findByCompanyIdAndNormalizedTitle(
                candidate.companyId(), candidate.normalizedTitle());
        for (Job job : exact) {
            if (!locationCompatible(job.getLocationCity(), candidate.locationCity())) {
                continue;
            }
            String evidence = evidence("normalized_title", candidate.normalizedTitle());
            if (alreadyClaimedBySameSource(candidate, job.getId())) {
                review = review != null ? review : new JobMatch.ReviewCandidate(job.getId(),
                        JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                        COMPANY_TITLE_LOCATION_CONFIDENCE, evidence);
                continue;
            }
            return Outcome.matched(JobMatch.matched(job.getId(),
                    JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                    COMPANY_TITLE_LOCATION_CONFIDENCE, evidence));
        }

        List<Long> nearIdentical = jobRepository.findSimilarTitleJobIds(
                candidate.companyId(), candidate.normalizedTitle(), TITLE_TRIGRAM_THRESHOLD, null, 5);
        for (Long jobId : nearIdentical) {
            Optional<Job> job = jobRepository.findById(jobId);
            if (job.isEmpty() || !locationCompatible(job.get().getLocationCity(), candidate.locationCity())) {
                continue;
            }
            String evidence = evidence("normalized_title_trigram", candidate.normalizedTitle());
            if (alreadyClaimedBySameSource(candidate, jobId)) {
                review = review != null ? review : new JobMatch.ReviewCandidate(jobId,
                        JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                        COMPANY_TITLE_LOCATION_CONFIDENCE, evidence);
                continue;
            }
            return Outcome.matched(JobMatch.matched(jobId,
                    JobSource.MatchMethod.COMPANY_TITLE_LOCATION,
                    COMPANY_TITLE_LOCATION_CONFIDENCE, evidence));
        }
        return Outcome.reviewOnly(review);
    }

    private Outcome matchByDescriptionSimilarity(JobMatchCandidate candidate) {
        String description = candidate.normalizedDescription();
        if (candidate.companyId() == null
                || description == null
                || description.length() < properties.minDescriptionLength()) {
            return Outcome.nothing();
        }
        JobMatch.ReviewCandidate review = null;

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
            String evidence = evidence("description_similarity",
                    String.format(Locale.ROOT, "%.3f", score));
            if (alreadyClaimedBySameSource(candidate, jobId)) {
                review = review != null ? review : new JobMatch.ReviewCandidate(jobId,
                        JobSource.MatchMethod.DESCRIPTION_SIMILARITY, confidence, evidence);
                continue;
            }
            return Outcome.matched(JobMatch.matched(jobId,
                    JobSource.MatchMethod.DESCRIPTION_SIMILARITY, confidence, evidence));
        }
        return Outcome.reviewOnly(review);
    }

    private boolean alreadyClaimedBySameSource(JobMatchCandidate candidate, Long jobId) {
        return jobSourceRepository.existsByJobIdAndSourceId(jobId, candidate.sourceId());
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

    private record Outcome(JobMatch match, JobMatch.ReviewCandidate review) {

        static Outcome nothing() {
            return new Outcome(null, null);
        }

        static Outcome matched(JobMatch match) {
            return new Outcome(match, null);
        }

        static Outcome reviewOnly(JobMatch.ReviewCandidate review) {
            return new Outcome(null, review);
        }
    }
}
