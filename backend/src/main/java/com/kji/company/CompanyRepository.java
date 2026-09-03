package com.kji.company;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNormalizedName(String normalizedName);

    Optional<Company> findByWebsiteDomain(String websiteDomain);

    @Query(value = """
            SELECT c.*, similarity(c.normalized_name, :candidate) AS score
            FROM companies c
            WHERE similarity(c.normalized_name, :candidate) >= :threshold
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Company> findSimilarByNormalizedName(@Param("candidate") String candidate,
                                              @Param("threshold") double threshold,
                                              @Param("limit") int limit);

    List<Company> findAllByOrderByCanonicalNameAsc(Pageable pageable);
}
