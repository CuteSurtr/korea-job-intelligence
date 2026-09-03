package com.kji.ingest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchRunRepository extends JpaRepository<SearchRun, Long> {

    Optional<SearchRun> findByRunUuid(UUID runUuid);

    Page<SearchRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<SearchRun> findBySourceIdOrderByStartedAtDesc(Long sourceId, Pageable pageable);
}
