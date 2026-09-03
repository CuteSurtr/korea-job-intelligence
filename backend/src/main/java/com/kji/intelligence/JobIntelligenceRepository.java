package com.kji.intelligence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobIntelligenceRepository extends JpaRepository<JobIntelligence, Long> {

    Optional<JobIntelligence> findByJobId(Long jobId);
}
