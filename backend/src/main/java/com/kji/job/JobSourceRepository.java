package com.kji.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobSourceRepository extends JpaRepository<JobSource, Long> {

    Optional<JobSource> findBySourceIdAndExternalKey(Long sourceId, String externalKey);

    Optional<JobSource> findFirstByCanonicalUrlKey(String canonicalUrlKey);

    Optional<JobSource> findFirstByExternalKey(String externalKey);

    List<JobSource> findByJobId(Long jobId);

    List<JobSource> findByJobIdIn(Collection<Long> jobIds);

    List<JobSource> findBySourceIdAndActiveTrue(Long sourceId);

    long countByJobId(Long jobId);

    boolean existsByJobIdAndSourceId(Long jobId, Long sourceId);

    @Query("select js.job.id from JobSource js where js.sourceId = :sourceId")
    List<Long> findJobIdsBySourceId(@Param("sourceId") Long sourceId);
}
