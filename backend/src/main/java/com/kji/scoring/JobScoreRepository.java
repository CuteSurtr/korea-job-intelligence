package com.kji.scoring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobScoreRepository extends JpaRepository<JobScore, Long> {

    List<JobScore> findByJobId(Long jobId);

    @Query("""
            select s from JobScore s
            where s.jobId = :jobId
              and s.scoreKind = :kind
              and s.scoreVersion = :version
              and (:profileId is null and s.profileId is null or s.profileId = :profileId)
            """)
    Optional<JobScore> findExisting(@Param("jobId") Long jobId,
                                    @Param("kind") JobScore.Kind kind,
                                    @Param("version") String version,
                                    @Param("profileId") Long profileId);
}
