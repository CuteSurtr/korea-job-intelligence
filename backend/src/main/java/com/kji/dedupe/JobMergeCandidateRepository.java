package com.kji.dedupe;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobMergeCandidateRepository extends JpaRepository<JobMergeCandidate, Long> {

    Optional<JobMergeCandidate> findByLeftJobIdAndRightJobId(Long leftJobId, Long rightJobId);

    List<JobMergeCandidate> findByStatusOrderByConfidenceDesc(JobMergeCandidate.Status status,
                                                              Pageable pageable);

    long countByStatus(JobMergeCandidate.Status status);
}
