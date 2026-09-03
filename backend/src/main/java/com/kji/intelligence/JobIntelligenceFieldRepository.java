package com.kji.intelligence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobIntelligenceFieldRepository extends JpaRepository<JobIntelligenceField, Long> {

    List<JobIntelligenceField> findByJobId(Long jobId);

    Optional<JobIntelligenceField> findByJobIdAndFieldNameAndExtractorVersion(
            Long jobId, String fieldName, String extractorVersion);
}
