package com.kji.search;

import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.JobSource;
import com.kji.job.JobSourceRepository;
import com.kji.job.LifecycleState;
import com.kji.source.SourceRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobSearchService {

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final SourceRepository sourceRepository;

    public JobSearchService(JobRepository jobRepository,
                            JobSourceRepository jobSourceRepository,
                            SourceRepository sourceRepository) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public Page<Job> search(JobSearchQuery query) {
        return jobRepository.findAll(toSpecification(query),
                PageRequest.of(query.page(), query.size(), toSort(query.sort())));
    }

    private Specification<Job> toSpecification(JobSearchQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.keyword() != null && !query.keyword().isBlank()) {
                String pattern = "%" + query.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("canonicalTitle")), pattern),
                        builder.like(builder.lower(root.get("normalizedTitle")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (query.company() != null && !query.company().isBlank()) {
                String pattern = "%" + query.company().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(
                        builder.lower(root.get("company").get("canonicalName")), pattern));
            }
            if (!query.lifecycleStates().isEmpty()) {
                predicates.add(root.get("lifecycleState").in(query.lifecycleStates()));
            } else if (Boolean.TRUE.equals(query.openOnly())) {
                predicates.add(builder.notEqual(root.get("lifecycleState"), LifecycleState.CLOSED));
            }
            if (query.locationCity() != null && !query.locationCity().isBlank()) {
                predicates.add(builder.equal(
                        builder.lower(root.get("locationCity")),
                        query.locationCity().trim().toLowerCase(Locale.ROOT)));
            }
            if (query.sourceCode() != null && !query.sourceCode().isBlank()) {
                criteriaQuery.distinct(true);
                predicates.add(root.get("id").in(
                        jobIdsForSource(query.sourceCode())));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<Long> jobIdsForSource(String sourceCode) {
        List<Long> ids = sourceRepository.findByCode(sourceCode)
                .map(source -> jobSourceRepository.findJobIdsBySourceId(source.getId()))
                .orElse(List.of());
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    private Sort toSort(JobSearchQuery.SortOrder order) {
        return switch (order) {
            case NEWEST -> Sort.by(Sort.Direction.DESC, "firstSeenAt");
            case RECENTLY_VERIFIED -> Sort.by(Sort.Direction.DESC, "lastVerifiedAt");
            case CLOSING_SOON -> Sort.by(Sort.Direction.ASC, "deadlineAt");
            case MOST_SOURCES -> Sort.by(Sort.Direction.DESC, "sourceCount");
            case COMPANY -> Sort.by(Sort.Direction.ASC, "company.canonicalName")
                    .and(Sort.by(Sort.Direction.ASC, "canonicalTitle"));
        };
    }

    List<JobSource> sourcesFor(Long jobId) {
        return jobSourceRepository.findByJobId(jobId);
    }
}
