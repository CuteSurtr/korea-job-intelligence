package com.kji.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "company_identifiers")
public class CompanyIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "identifier_type", nullable = false, length = 48)
    private String identifierType;

    @Column(name = "identifier_value", nullable = false, length = 320)
    private String identifierValue;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    protected CompanyIdentifier() {
    }

    public CompanyIdentifier(Company company, String identifierType, String identifierValue,
                             Long sourceId, Instant observedAt) {
        this.company = company;
        this.identifierType = identifierType;
        this.identifierValue = identifierValue;
        this.sourceId = sourceId;
        this.observedAt = observedAt;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}
