package com.kji.job;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobLifecycleEventRepository extends JpaRepository<JobLifecycleEvent, Long> {

    List<JobLifecycleEvent> findByJobIdOrderByOccurredAtDesc(Long jobId);
}
