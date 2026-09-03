package com.kji.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "company_aliases")
public class CompanyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 320)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, length = 320, unique = true)
    private String normalizedAlias;

    @Enumerated(EnumType.STRING)
    @Column(name = "alias_kind", nullable = false, length = 32)
    private AliasKind aliasKind = AliasKind.OBSERVED;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    protected CompanyAlias() {
    }

    public CompanyAlias(Company company, String alias, String normalizedAlias,
                        AliasKind aliasKind, Long sourceId, Instant firstSeenAt) {
        this.company = company;
        this.alias = alias;
        this.normalizedAlias = normalizedAlias;
        this.aliasKind = aliasKind;
        this.sourceId = sourceId;
        this.firstSeenAt = firstSeenAt;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getAlias() {
        return alias;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }

    public AliasKind getAliasKind() {
        return aliasKind;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public enum AliasKind {
        LEGAL,
        BRAND,
        ROMANIZED,
        OBSERVED,
        MANUAL
    }
}
