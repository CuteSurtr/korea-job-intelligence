package com.kji.crm;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobIdAndProfileId(Long jobId, Long profileId);

    Page<Application> findByProfileIdOrderByUpdatedAtDesc(Long profileId, Pageable pageable);

    Page<Application> findByProfileIdAndStatusOrderByUpdatedAtDesc(
            Long profileId, ApplicationStatus status, Pageable pageable);

    List<Application> findByJobId(Long jobId);

    long countByProfileIdAndStatus(Long profileId, ApplicationStatus status);
}
