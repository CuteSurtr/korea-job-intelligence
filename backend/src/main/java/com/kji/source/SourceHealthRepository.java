package com.kji.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SourceHealthRepository extends JpaRepository<SourceHealth, Long> {

    Optional<SourceHealth> findBySourceId(Long sourceId);

    @Query("select h from SourceHealth h join fetch h.source order by h.source.code asc")
    List<SourceHealth> findAllWithSource();
}
