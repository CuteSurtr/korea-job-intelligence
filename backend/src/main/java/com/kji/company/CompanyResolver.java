package com.kji.company;

import com.kji.normalize.CompanyNameNormalizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyResolver {

    private static final double TRIGRAM_THRESHOLD = 0.92d;
    private static final int TRIGRAM_CANDIDATE_LIMIT = 5;

    private final CompanyRepository companyRepository;
    private final CompanyAliasRepository aliasRepository;
    private final CompanyIdentifierRepository identifierRepository;
    private final CompanyNameNormalizer nameNormalizer;

    public CompanyResolver(CompanyRepository companyRepository,
                           CompanyAliasRepository aliasRepository,
                           CompanyIdentifierRepository identifierRepository,
                           CompanyNameNormalizer nameNormalizer) {
        this.companyRepository = companyRepository;
        this.aliasRepository = aliasRepository;
        this.identifierRepository = identifierRepository;
        this.nameNormalizer = nameNormalizer;
    }

    @Transactional
    public CompanyResolution resolve(String rawCompanyName,
                                     Map<String, String> providerIdentifiers,
                                     Long sourceId,
                                     Instant observedAt) {
        Optional<CompanyResolution> byIdentifier = resolveByIdentifier(providerIdentifiers);
        if (byIdentifier.isPresent()) {
            recordAlias(byIdentifier.get().company(), rawCompanyName, sourceId, observedAt);
            return byIdentifier.get();
        }

        String normalized = nameNormalizer.normalize(rawCompanyName);
        if (normalized == null || normalized.isBlank()) {
            throw new CompanyResolutionException("Company name is missing or normalizes to empty");
        }

        Optional<Company> exact = companyRepository.findByNormalizedName(normalized);
        if (exact.isPresent()) {
            Company company = exact.get();
            linkIdentifiers(company, providerIdentifiers, sourceId, observedAt);
            return new CompanyResolution(company, false,
                    CompanyResolution.Method.EXACT_NORMALIZED_NAME, 1.0d);
        }

        Optional<CompanyAlias> alias = aliasRepository.findByNormalizedAlias(normalized);
        if (alias.isPresent()) {
            Company company = alias.get().getCompany();
            linkIdentifiers(company, providerIdentifiers, sourceId, observedAt);
            return new CompanyResolution(company, false, CompanyResolution.Method.ALIAS, 0.95d);
        }

        List<Company> similar = companyRepository.findSimilarByNormalizedName(
                normalized, TRIGRAM_THRESHOLD, TRIGRAM_CANDIDATE_LIMIT);
        if (!similar.isEmpty()) {
            Company company = similar.get(0);
            recordAlias(company, rawCompanyName, sourceId, observedAt);
            linkIdentifiers(company, providerIdentifiers, sourceId, observedAt);
            return new CompanyResolution(company, false,
                    CompanyResolution.Method.TRIGRAM_SIMILARITY, TRIGRAM_THRESHOLD);
        }

        Company created = companyRepository.save(new Company(
                nameNormalizer.canonicalDisplayName(rawCompanyName), normalized, "KR"));
        recordAlias(created, rawCompanyName, sourceId, observedAt);
        linkIdentifiers(created, providerIdentifiers, sourceId, observedAt);
        return new CompanyResolution(created, true, CompanyResolution.Method.CREATED, 1.0d);
    }

    private Optional<CompanyResolution> resolveByIdentifier(Map<String, String> providerIdentifiers) {
        if (providerIdentifiers == null || providerIdentifiers.isEmpty()) {
            return Optional.empty();
        }
        for (Map.Entry<String, String> entry : providerIdentifiers.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            Optional<CompanyIdentifier> match = identifierRepository
                    .findByIdentifierTypeAndIdentifierValue(entry.getKey(), entry.getValue());
            if (match.isPresent()) {
                return Optional.of(new CompanyResolution(match.get().getCompany(), false,
                        CompanyResolution.Method.PROVIDER_IDENTIFIER, 1.0d));
            }
        }
        return Optional.empty();
    }

    private void linkIdentifiers(Company company, Map<String, String> providerIdentifiers,
                                 Long sourceId, Instant observedAt) {
        if (providerIdentifiers == null) {
            return;
        }
        providerIdentifiers.forEach((type, value) -> {
            if (value == null || value.isBlank()) {
                return;
            }
            if (identifierRepository.findByIdentifierTypeAndIdentifierValue(type, value).isEmpty()) {
                identifierRepository.save(
                        new CompanyIdentifier(company, type, value, sourceId, observedAt));
            }
        });
    }

    private void recordAlias(Company company, String rawCompanyName, Long sourceId, Instant observedAt) {
        String normalized = nameNormalizer.normalize(rawCompanyName);
        if (normalized == null || normalized.isBlank()) {
            return;
        }
        if (normalized.equals(company.getNormalizedName())) {
            return;
        }
        if (aliasRepository.findByNormalizedAlias(normalized).isPresent()) {
            return;
        }
        aliasRepository.save(new CompanyAlias(company, rawCompanyName.trim(), normalized,
                CompanyAlias.AliasKind.OBSERVED, sourceId, observedAt));
    }
}
