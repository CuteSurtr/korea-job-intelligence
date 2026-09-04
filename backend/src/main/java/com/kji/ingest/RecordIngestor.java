package com.kji.ingest;

import com.kji.company.CompanyResolution;
import com.kji.company.CompanyResolver;
import com.kji.dedupe.JobMatch;
import com.kji.dedupe.JobMatchCandidate;
import com.kji.dedupe.JobMatcher;
import com.kji.dedupe.JobMergeCandidate;
import com.kji.dedupe.JobMergeCandidateRepository;
import com.kji.job.Job;
import com.kji.job.JobLifecycleService;
import com.kji.job.JobRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import com.kji.normalize.Deadline;
import com.kji.normalize.DeadlineParser;
import com.kji.normalize.Extracted;
import com.kji.normalize.LocationNormalizer;
import com.kji.normalize.TextNormalizer;
import com.kji.normalize.TitleNormalizer;
import com.kji.snapshot.JobSnapshot;
import com.kji.snapshot.JobSnapshotRepository;
import com.kji.source.AdapterKind;
import com.kji.source.RawJobRecord;
import com.kji.source.Source;
import com.kji.source.SourceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordIngestor {

    private final CompanyResolver companyResolver;
    private final JobMatcher jobMatcher;
    private final JobMergeCandidateRepository mergeCandidateRepository;
    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSnapshotRepository snapshotRepository;
    private final JobLifecycleService lifecycleService;
    private final TitleNormalizer titleNormalizer;
    private final SourceRepository sourceRepository;
    private final LocationNormalizer locationNormalizer;
    private final DeadlineParser deadlineParser;
    private final JobEnrichmentService enrichmentService;

    public RecordIngestor(CompanyResolver companyResolver,
                          JobMatcher jobMatcher,
                          JobMergeCandidateRepository mergeCandidateRepository,
                          JobRepository jobRepository,
                          JobSourceRepository jobSourceRepository,
                          JobSnapshotRepository snapshotRepository,
                          JobLifecycleService lifecycleService,
                          TitleNormalizer titleNormalizer,
                          SourceRepository sourceRepository,
                          LocationNormalizer locationNormalizer,
                          DeadlineParser deadlineParser,
                          JobEnrichmentService enrichmentService) {
        this.companyResolver = companyResolver;
        this.jobMatcher = jobMatcher;
        this.mergeCandidateRepository = mergeCandidateRepository;
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.snapshotRepository = snapshotRepository;
        this.lifecycleService = lifecycleService;
        this.titleNormalizer = titleNormalizer;
        this.sourceRepository = sourceRepository;
        this.locationNormalizer = locationNormalizer;
        this.deadlineParser = deadlineParser;
        this.enrichmentService = enrichmentService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecordIngestionResult ingest(Source source, SearchRun run, RawJobRecord record) {
        String externalKey = record.externalKey();

        String canonicalTitle = titleNormalizer.canonicalTitle(record.rawTitle());
        String normalizedTitle = titleNormalizer.normalize(record.rawTitle());
        LocationNormalizer.NormalizedLocation location = locationNormalizer.normalize(record.rawLocation());
        String description = record.rawDescription();
        String normalizedDescription = TextNormalizer.normalizeForMatching(description);
        String canonicalUrl = record.canonicalApplyUrl().orElse(null);
        String canonicalUrlKey = record.canonicalUrlKey().orElse(null);

        CompanyResolution company = companyResolver.resolve(
                record.rawCompany(), record.companyIdentifiers(), source.getId(), record.fetchedAt());

        SnapshotWrite snapshotWrite = writeSnapshot(source, run, record, externalKey);

        Optional<JobSource> existing =
                jobSourceRepository.findBySourceIdAndExternalKey(source.getId(), externalKey);

        Job job;
        JobSource jobSource;
        RecordIngestionResult.Outcome outcome;

        if (existing.isPresent()) {
            jobSource = existing.get();
            job = jobSource.getJob();
            jobSource.observe(record.fetchedAt(), record.sourceUrl(), canonicalUrl,
                    canonicalUrlKey, snapshotWrite.snapshot().getId());
            jobSourceRepository.save(jobSource);
            outcome = RecordIngestionResult.Outcome.JOB_UPDATED;
        } else {
            JobMatch match = jobMatcher.match(new JobMatchCandidate(
                    company.company().getId(),
                    source.getId(),
                    source.isStableExternalId(),
                    externalKey,
                    canonicalUrlKey,
                    normalizedTitle,
                    normalizedDescription,
                    location.city()));

            if (match.matched()) {
                job = jobRepository.findById(match.jobId()).orElseThrow(() ->
                        new IllegalStateException("Matched job " + match.jobId() + " no longer exists"));
                outcome = RecordIngestionResult.Outcome.DUPLICATE_MERGED;
            } else {
                job = jobRepository.save(new Job(company.company(), canonicalTitle, normalizedTitle,
                        canonicalUrl, canonicalUrlKey, record.fetchedAt(), source.getId()));
                outcome = RecordIngestionResult.Outcome.JOB_CREATED;
                queueForReview(match.reviewCandidate(), job.getId(), record.fetchedAt());
            }

            jobSource = jobSourceRepository.save(new JobSource(
                    job, source.getId(), record.externalId(), externalKey,
                    record.sourceUrl(), canonicalUrl, canonicalUrlKey, record.fetchedAt(),
                    match.method(), confidence(match.confidence()), match.evidence()));
            jobSource.observe(record.fetchedAt(), record.sourceUrl(), canonicalUrl,
                    canonicalUrlKey, snapshotWrite.snapshot().getId());
            jobSourceRepository.save(jobSource);
        }

        applyCanonicalUpgrade(job, source, canonicalTitle, normalizedTitle, canonicalUrl, canonicalUrlKey);
        job.updateLocation(location.raw(), location.city(), location.region(), location.countryCode());
        job.updateDescription(description, normalizedDescription);
        job.updatePostedAt(record.postedAt());
        applyDeadline(job, record);
        jobRepository.save(job);

        snapshotWrite.snapshot().attachTo(job.getId(), jobSource.getId());
        snapshotRepository.save(snapshotWrite.snapshot());

        lifecycleService.recordObservation(job, jobSource, run.getId(), record.fetchedAt(),
                snapshotWrite.snapshot().getId(), snapshotWrite.contentChanged());
        lifecycleService.refreshSourceCount(job);

        enrichmentService.enrich(job, record, snapshotWrite.snapshot().getId(), source.getId(),
                source.getAdapterKind() == AdapterKind.ATS);

        jobRepository.updateSearchDocument(job.getId(), job.getCanonicalTitle(),
                job.getCompany().getCanonicalName(), job.getDescription());

        return new RecordIngestionResult(outcome, job.getId(), externalKey);
    }

    private void queueForReview(JobMatch.ReviewCandidate review, Long newJobId, Instant detectedAt) {
        if (review == null || review.jobId() == null || review.jobId().equals(newJobId)) {
            return;
        }
        Long left = Math.min(review.jobId(), newJobId);
        Long right = Math.max(review.jobId(), newJobId);
        if (mergeCandidateRepository.findByLeftJobIdAndRightJobId(left, right).isPresent()) {
            return;
        }
        mergeCandidateRepository.save(JobMergeCandidate.between(left, right,
                review.method().name(), confidence(review.confidence()), review.evidence(),
                detectedAt));
    }

    private SnapshotWrite writeSnapshot(Source source, SearchRun run, RawJobRecord record, String externalKey) {
        String contentHash = record.contentHash();
        Optional<JobSnapshot> existing = snapshotRepository
                .findBySourceIdAndExternalKeyAndContentHash(source.getId(), externalKey, contentHash);
        if (existing.isPresent()) {
            return new SnapshotWrite(existing.get(), false);
        }
        JobSnapshot snapshot = JobSnapshot.builder()
                .sourceId(source.getId())
                .searchRunId(run.getId())
                .externalId(record.externalId())
                .externalKey(externalKey)
                .sourceUrl(record.sourceUrl())
                .originalApplyUrl(record.originalApplyUrl())
                .fetchedAt(record.fetchedAt())
                .rawTitle(record.rawTitle())
                .rawCompany(record.rawCompany())
                .rawLocation(record.rawLocation())
                .rawDescription(record.rawDescription())
                .rawRequirements(record.rawRequirements())
                .rawEmploymentType(record.rawEmploymentType())
                .rawExperience(record.rawExperience())
                .rawEducation(record.rawEducation())
                .rawDeadline(record.rawDeadline())
                .rawPayload(record.rawPayload() == null ? "{}" : record.rawPayload().toString())
                .payloadHash(record.payloadHash())
                .contentHash(contentHash)
                .build();
        return new SnapshotWrite(snapshotRepository.save(snapshot), true);
    }

    private void applyCanonicalUpgrade(Job job, Source source, String canonicalTitle,
                                       String normalizedTitle, String canonicalUrl, String canonicalUrlKey) {
        boolean noPrimary = job.getPrimarySourceId() == null;
        boolean sameSource = !noPrimary && job.getPrimarySourceId().equals(source.getId());
        boolean higherTrust = !noPrimary && !sameSource && isHigherTrust(source, job.getPrimarySourceId());

        if (noPrimary || sameSource || higherTrust) {
            String urlKey = canonicalUrlKey;
            if (urlKey != null && !urlKey.equals(job.getCanonicalUrlKey())
                    && jobRepository.findByCanonicalUrlKey(urlKey)
                    .filter(other -> !other.getId().equals(job.getId()))
                    .isPresent()) {
                urlKey = job.getCanonicalUrlKey();
            }
            job.updateCanonicalFields(
                    canonicalTitle != null ? canonicalTitle : job.getCanonicalTitle(),
                    normalizedTitle != null ? normalizedTitle : job.getNormalizedTitle(),
                    canonicalUrl != null ? canonicalUrl : job.getCanonicalApplyUrl(),
                    urlKey,
                    higherTrust || noPrimary ? source.getId() : job.getPrimarySourceId());
        }
    }

    private boolean isHigherTrust(Source candidate, Long currentPrimarySourceId) {
        return sourceRepository.findById(currentPrimarySourceId)
                .map(current -> candidate.getTrustTier() < current.getTrustTier())
                .orElse(true);
    }

    private void applyDeadline(Job job, RawJobRecord record) {
        Extracted<Deadline> parsed = deadlineParser.parse(record.rawDeadline(), record.fetchedAt());
        if (!parsed.isKnown()) {
            return;
        }
        Deadline deadline = parsed.value();
        job.updateDeadline(deadline.closesAt(), deadline.openEnded());
    }

    private BigDecimal confidence(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }

    private record SnapshotWrite(JobSnapshot snapshot, boolean contentChanged) {
    }
}
