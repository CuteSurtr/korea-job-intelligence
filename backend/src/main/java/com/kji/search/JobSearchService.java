package com.kji.search;

import com.kji.company.Company;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.job.JobSourceRepository;
import com.kji.job.LifecycleState;
import com.kji.source.SourceRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final Clock clock;

    public JobSearchService(JobRepository jobRepository,
                            JobSourceRepository jobSourceRepository,
                            SourceRepository sourceRepository,
                            Clock clock) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.sourceRepository = sourceRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Job> search(JobSearchQuery query) {
        return jobRepository.findAll(toSpecification(query),
                PageRequest.of(query.page(), query.size(), toSort(query.sort())));
    }

    private Specification<Job> toSpecification(JobSearchQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (notBlank(query.keyword())) {
                String pattern = "%" + query.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("canonicalTitle")), pattern),
                        builder.like(builder.lower(root.get("normalizedTitle")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (notBlank(query.company())) {
                String pattern = "%" + query.company().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(
                        builder.lower(root.get("company").get("canonicalName")), pattern));
            }
            if (!query.lifecycleStates().isEmpty()) {
                predicates.add(root.get("lifecycleState").in(query.lifecycleStates()));
            } else if (Boolean.TRUE.equals(query.openOnly())) {
                predicates.add(builder.notEqual(root.get("lifecycleState"), LifecycleState.CLOSED));
            }
            if (notBlank(query.locationCity())) {
                predicates.add(builder.equal(builder.lower(root.get("locationCity")),
                        query.locationCity().trim().toLowerCase(Locale.ROOT)));
            }
            if (notBlank(query.roleFamily())) {
                predicates.add(builder.equal(root.get("roleFamily"),
                        query.roleFamily().trim().toUpperCase(Locale.ROOT)));
            }
            if (!query.seniorityBuckets().isEmpty()) {
                predicates.add(root.get("seniorityBucket").in(query.seniorityBuckets()));
            }
            if (query.maxYearsExperience() != null) {
                predicates.add(builder.or(
                        builder.isNull(root.get("yearsExperienceMin")),
                        builder.lessThanOrEqualTo(root.get("yearsExperienceMin"),
                                query.maxYearsExperience())));
            }
            if (query.minCareerValue() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("careerValueScore"),
                        BigDecimal.valueOf(query.minCareerValue())));
            }
            if (query.minCandidateFit() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("candidateFitScore"),
                        BigDecimal.valueOf(query.minCandidateFit())));
            }
            if (notBlank(query.remotePolicy())) {
                predicates.add(builder.equal(root.get("remotePolicy"),
                        query.remotePolicy().trim().toUpperCase(Locale.ROOT)));
            }
            if (notBlank(query.degreeRequired())) {
                predicates.add(builder.equal(root.get("degreeRequired"),
                        query.degreeRequired().trim().toUpperCase(Locale.ROOT)));
            }
            if (notBlank(query.companyRiskLevel())) {
                predicates.add(builder.equal(root.get("company").get("riskLevel"),
                        Company.RiskLevel.valueOf(
                                query.companyRiskLevel().trim().toUpperCase(Locale.ROOT))));
            }
            if (query.postedWithinDays() != null) {
                Instant threshold = Instant.now(clock)
                        .minus(query.postedWithinDays(), ChronoUnit.DAYS);
                predicates.add(builder.or(
                        builder.greaterThanOrEqualTo(root.get("postedAt"), threshold),
                        builder.and(builder.isNull(root.get("postedAt")),
                                builder.greaterThanOrEqualTo(root.get("firstSeenAt"), threshold))));
            }
            if (notBlank(query.sourceCode())) {
                predicates.add(root.get("id").in(jobIdsForSource(query.sourceCode())));
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
            case BEST_MATCH -> Sort.by(Sort.Order.desc("applicationPriorityScore").nullsLast(),
                    Sort.Order.desc("firstSeenAt"));
            case HIGHEST_CAREER_VALUE -> Sort.by(Sort.Order.desc("careerValueScore").nullsLast(),
                    Sort.Order.desc("firstSeenAt"));
            case JUNIOR_FRIENDLY -> Sort.by(Sort.Order.asc("seniorityBucket").nullsLast(),
                    Sort.Order.desc("candidateFitScore").nullsLast());
            case NEWEST -> Sort.by(Sort.Order.desc("firstSeenAt"));
            case CLOSING_SOON -> Sort.by(Sort.Order.asc("deadlineAt").nullsLast());
            case RECENTLY_VERIFIED -> Sort.by(Sort.Order.desc("lastVerifiedAt").nullsLast());
            case MOST_SOURCES -> Sort.by(Sort.Order.desc("sourceCount"),
                    Sort.Order.desc("firstSeenAt"));
            case COMPANY -> Sort.by(Sort.Order.asc("company.canonicalName"),
                    Sort.Order.asc("canonicalTitle"));
        };
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
