package com.kji.job;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobVerificationRepository extends JpaRepository<JobVerification, Long> {

    List<JobVerification> findByJobIdOrderByVerifiedAtDesc(Long jobId, Pageable pageable);
}
