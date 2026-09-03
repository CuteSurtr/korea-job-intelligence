package com.kji.company;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyAliasRepository extends JpaRepository<CompanyAlias, Long> {

    Optional<CompanyAlias> findByNormalizedAlias(String normalizedAlias);

    List<CompanyAlias> findByCompanyId(Long companyId);
}
