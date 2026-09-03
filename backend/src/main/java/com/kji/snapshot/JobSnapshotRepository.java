package com.kji.snapshot;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSnapshotRepository extends JpaRepository<JobSnapshot, Long> {

    Optional<JobSnapshot> findBySourceIdAndExternalKeyAndContentHash(Long sourceId,
                                                                    String externalKey,
                                                                    String contentHash);

    List<JobSnapshot> findByJobIdOrderByFetchedAtDesc(Long jobId);

    Optional<JobSnapshot> findFirstBySourceIdAndExternalKeyOrderByFetchedAtDesc(Long sourceId,
                                                                               String externalKey);
}
