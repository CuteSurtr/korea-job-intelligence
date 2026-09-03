package com.kji.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMetricRepository extends JpaRepository<CompanyMetric, Long> {

    List<CompanyMetric> findByCompanyIdOrderByObservedAtDesc(Long companyId);
}
