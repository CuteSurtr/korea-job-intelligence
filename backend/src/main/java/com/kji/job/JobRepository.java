package com.kji.job;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByCanonicalUrlKey(String canonicalUrlKey);

    List<Job> findByCompanyIdAndNormalizedTitle(Long companyId, String normalizedTitle);

    List<Job> findByCompanyId(Long companyId);

    Page<Job> findByLifecycleStateIn(List<LifecycleState> states, Pageable pageable);

    @Query("""
            select j from Job j
            where j.lifecycleState in :states
              and (j.lastVerifiedAt is null or j.lastVerifiedAt < :threshold)
            """)
    List<Job> findVerificationStale(@Param("states") List<LifecycleState> states,
                                    @Param("threshold") Instant threshold);

    @Modifying
    @Query(value = """
            UPDATE jobs
            SET search_document = to_tsvector('simple',
                coalesce(:title, '') || ' ' || coalesce(:company, '') || ' ' || coalesce(:description, ''))
            WHERE id = :jobId
            """, nativeQuery = true)
    void updateSearchDocument(@Param("jobId") Long jobId,
                              @Param("title") String title,
                              @Param("company") String company,
                              @Param("description") String description);

    @Query(value = """
            SELECT j.id
            FROM jobs j
            WHERE j.company_id = :companyId
              AND j.id <> coalesce(:excludeJobId, -1)
              AND similarity(j.normalized_title, :normalizedTitle) >= :threshold
            ORDER BY similarity(j.normalized_title, :normalizedTitle) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findSimilarTitleJobIds(@Param("companyId") Long companyId,
                                      @Param("normalizedTitle") String normalizedTitle,
                                      @Param("threshold") double threshold,
                                      @Param("excludeJobId") Long excludeJobId,
                                      @Param("limit") int limit);

    @Query(value = """
            SELECT j.id, similarity(j.normalized_description, :normalizedDescription) AS score
            FROM jobs j
            WHERE j.company_id = :companyId
              AND j.normalized_description IS NOT NULL
              AND length(j.normalized_description) >= :minLength
              AND similarity(j.normalized_description, :normalizedDescription) >= :threshold
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarDescriptionJobs(@Param("companyId") Long companyId,
                                              @Param("normalizedDescription") String normalizedDescription,
                                              @Param("minLength") int minLength,
                                              @Param("threshold") double threshold,
                                              @Param("limit") int limit);
}
