package com.kji.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRiskReasonRepository extends JpaRepository<CompanyRiskReason, Long> {

    List<CompanyRiskReason> findByCompanyIdOrderByAssessedAtDesc(Long companyId);
}
