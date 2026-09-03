package com.kji.ingest;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionFailureRepository extends JpaRepository<IngestionFailure, Long> {

    List<IngestionFailure> findBySearchRunIdOrderByOccurredAtAsc(Long searchRunId);

    List<IngestionFailure> findBySourceIdOrderByOccurredAtDesc(Long sourceId, Pageable pageable);

    long countBySearchRunId(Long searchRunId);
}
