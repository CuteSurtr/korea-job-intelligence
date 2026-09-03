package com.kji.web;

import com.kji.job.Job;
import com.kji.job.JobLifecycleEventRepository;
import com.kji.job.JobRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import com.kji.job.JobVerificationRepository;
import com.kji.snapshot.JobSnapshot;
import com.kji.snapshot.JobSnapshotRepository;
import com.kji.source.Source;
import com.kji.source.SourceRepository;
import com.kji.web.dto.JobDetailResponse;
import com.kji.web.dto.JobResponse;
import com.kji.web.dto.JobSourceResponse;
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
    private final SourceRepository sourceRepository;

    public JobDetailAssembler(JobRepository jobRepository,
                              JobSourceRepository jobSourceRepository,
                              JobSnapshotRepository snapshotRepository,
                              JobVerificationRepository verificationRepository,
                              JobLifecycleEventRepository lifecycleEventRepository,
                              SourceRepository sourceRepository) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.snapshotRepository = snapshotRepository;
        this.verificationRepository = verificationRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public JobDetailResponse assemble(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No job with id " + jobId));

        Map<Long, String> sourceCodes = new HashMap<>();
        sourceRepository.findAll().forEach(source -> sourceCodes.put(source.getId(), source.getCode()));

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

        return new JobDetailResponse(JobResponse.from(job), job.getDescription(), sources,
                snapshots, verifications, lifecycle);
    }

    @Transactional(readOnly = true)
    public List<JobSource> sourcesFor(Long jobId) {
        return jobSourceRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> sourceCodesById() {
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
