package com.kji.company;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyIdentifierRepository extends JpaRepository<CompanyIdentifier, Long> {

    Optional<CompanyIdentifier> findByIdentifierTypeAndIdentifierValue(String identifierType,
                                                                       String identifierValue);

    List<CompanyIdentifier> findByCompanyId(Long companyId);
}
