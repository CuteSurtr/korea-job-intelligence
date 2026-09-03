package com.kji.job;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSightingRepository extends JpaRepository<JobSighting, Long> {

    List<JobSighting> findByJobIdOrderBySeenAtDesc(Long jobId, Pageable pageable);

    long countByJobId(Long jobId);
}
