package com.kji.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByCode(String code);

    List<Source> findByEnabledTrueOrderByTrustTierAscCodeAsc();

    List<Source> findByEnabledTrueAndRuntimeAvailableTrueOrderByCodeAsc();
}
